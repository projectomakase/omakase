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
package org.projectomakase.omakase.rest.version;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.jboss.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * JAX-RS {@link javax.ws.rs.container.ContainerRequestFilter} implementation used to process the custom API-Version request header. The processing is done prior to matching the JAX-RS resource.
 * <p>
 * I the API-Version header is set and the Content-Type header does NOT container a version the Content-Type header is amended to use the version in the API-Version header.
 * </p>
 *
 * @author Richard Lucas
 */
@PreMatching
public class APIVersionRequestFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(APIVersionRequestFilter.class);
    private static final Pattern VERSION = Pattern.compile("v[0-9]");
    private static final Pattern VERSION_JSON = Pattern.compile("v[0-9]\\+json");
    private static final ImmutableList<String> VERSIONS = ImmutableList.of("v1");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Applying API Version filter to request " + requestContext.getUriInfo().getRequestUri().toString());
        }

        String apiVersion = requestContext.getHeaderString("API-Version");

        if (!Strings.isNullOrEmpty(apiVersion)) {

            if (!VERSION.matcher(apiVersion).matches()) {
                throw new WebApplicationException("Invalid API version format, expected v[0-9] " + apiVersion, Response.Status.BAD_REQUEST);
            }

            if (!VERSIONS.contains(apiVersion)) {
                throw new WebApplicationException("Invalid API version " + apiVersion, Response.Status.BAD_REQUEST);
            }

            String contentType = requestContext.getHeaderString("Content-Type");

            String versionedContentType = getVersionedContentType(apiVersion, contentType);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Setting Content-Type to " + versionedContentType);
            }

            requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, versionedContentType);

        }
    }

    private static String getVersionedContentType(String apiVersion, String contentType) {
        String versionedContentType;

        if (Strings.isNullOrEmpty(contentType)) {
            versionedContentType = "application/" + apiVersion + "+json";
        } else if (VERSION_JSON.matcher(contentType).find()) {
            versionedContentType = contentType;
        } else if (contentType.endsWith("+json")) {
            versionedContentType = contentType.replace("+json", "." + apiVersion + "+json");
        } else if (contentType.endsWith("json")) {
            versionedContentType = contentType.replace("json", apiVersion + "+json");
        } else {
            versionedContentType = "application/" + apiVersion + "+json";
        }
        return versionedContentType;
    }
}
