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
package org.projectomakase.omakase.rest.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.collect.ImmutableSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;

/**
 * @author Richard Lucas
 */
@Provider
@Consumes({"application/*+json"})
@Produces({"application/*+json"})
public class JsonProvider extends JacksonJaxbJsonProvider {

    private static final Version VERSION = new Version(1, 0, 0, null, null, null);

    public JsonProvider() {
        super();
        super.setAnnotationsToUse(DEFAULT_ANNOTATIONS);
        super.setMapper(getObjectMapper());
    }

    @Override
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        // Workaround for a null-pointer issue that occurs when serializing entities in an ExceptionMapper
        Annotation[] writeToAnnotationArray = annotations;
        if (writeToAnnotationArray == null) {
            writeToAnnotationArray = new Annotation[0];
        }
        super.writeTo(value, type, genericType, writeToAnnotationArray, mediaType, httpHeaders, entityStream);
    }

    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

        ImmutableSet<SimpleModule> modules = getModules();
        if (getModules() != null) {
            modules.stream().forEach(mapper::registerModule);
        }

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        mapper.registerModule(new GuavaModule());
        return mapper;
    }

    private ImmutableSet<SimpleModule> getModules() {
        ImmutableSet.Builder<SimpleModule> builder = ImmutableSet.builder();

        // add custom support for Java 8 Date Times (the Jackson extension (v2.4.0) is de-serializing the date as GMT)
        SimpleModule java8DateTimeModule = new SimpleModule(VERSION);
        java8DateTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        java8DateTimeModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        builder.add(java8DateTimeModule);

        builder.add(new SimpleModule("JCR PropertyDefinition Module", VERSION).addSerializer(new PropertyDefinitionSerializer()));

        return builder.build();
    }
}
