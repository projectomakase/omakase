/*
 * #%L
 * omakase
 * %%
 * Copyright (C) 2015 Project Omakase LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.projectomakase.omakase.rest.patch;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.collect.ImmutableSet;
import org.jboss.resteasy.specimpl.BuiltResponse;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author Richard Lucas
 */
@PATCH
public class JsonPatchInterceptor implements ReaderInterceptor {

    @Context
    UriInfo uriInfo;

    @Context
    Providers providers;

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        Optional<Object> optionalBean = getCurrentObject();

        if (optionalBean.isPresent()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Currently assume the bean is of type BuiltResponse, this may need to change in the future
            Object bean = ((BuiltResponse) optionalBean.get()).getEntity();

            MessageBodyWriter<? super Object> bodyWriter = providers.getMessageBodyWriter(Object.class, bean.getClass(), new Annotation[0], MediaType.APPLICATION_JSON_TYPE);
            bodyWriter.writeTo(bean, bean.getClass(), bean.getClass(), new Annotation[0], MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), outputStream);

            // Use the Jackson 2.x classes to convert both the incoming patch and the current state of the object into a JsonNode / JsonPatch
            ObjectMapper mapper = new ObjectMapper();
            JsonNode serverState = mapper.readValue(outputStream.toByteArray(), JsonNode.class);
            JsonNode patchAsNode = mapper.readValue(context.getInputStream(), JsonNode.class);
            JsonPatch patch = JsonPatch.fromJson(patchAsNode);

            try {
                JsonNode result = patch.apply(serverState);
                // Stream the result & modify the stream on the readerInterceptor
                ByteArrayOutputStream resultAsByteArray = new ByteArrayOutputStream();
                mapper.writeValue(resultAsByteArray, result);
                context.setInputStream(new ByteArrayInputStream(resultAsByteArray.toByteArray()));

                return context.proceed();

            } catch (JsonPatchException | JsonMappingException e) {
                throw new WebApplicationException(e.getMessage(), e, Response.status(Response.Status.BAD_REQUEST).build());
            }

        } else {
            throw new IllegalArgumentException("No matching GET method on resource");
        }
    }

    private Optional<Object> getCurrentObject() {

        Optional<Object> bean = Optional.empty();

        // Assume that the first GET method on the resource without a Path can be used to get the current object. This is somewhat fragile but the integration tests will verify that this assumption is correct.
        Object resource = uriInfo.getMatchedResources().get(0);

        Optional<Method> found = getResourceGetMethod(resource);

        if (found.isPresent()) {
            try {
                ImmutableSet.Builder<String> argumentsBuilder = ImmutableSet.builder();
                uriInfo.getPathParameters().forEach((k, v) -> argumentsBuilder.add(v.get(0)));
                ImmutableSet<String> arguments = argumentsBuilder.build();
                bean = Optional.ofNullable(found.get().invoke(resource, (Object[]) arguments.toArray(new Object[arguments.size()])));
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof NotFoundException) {
                    throw (NotFoundException) e.getCause();
                } else {
                    throw new WebApplicationException(e);
                }
            } catch (Exception e) {
                throw new WebApplicationException(e);
            }
        }
        return bean;
    }

    private Optional<Method> getResourceGetMethod(Object resource) {
        Optional<Method> found = Optional.empty();
        for (Method next : resource.getClass().getMethods()) {
            if (next.getAnnotation(GET.class) != null && next.getAnnotation(Path.class) == null) {
                found = Optional.of(next);
                break;
            }
        }
        return found;
    }


}
