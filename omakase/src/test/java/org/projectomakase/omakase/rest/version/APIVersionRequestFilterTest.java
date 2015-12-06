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

import org.jboss.resteasy.core.interception.PreMatchContainerRequestContext;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class APIVersionRequestFilterTest {

    private APIVersionRequestFilter requestFilter;
    private ContainerRequestContext requestContext;

    @Before
    public void before() {
        requestFilter = new APIVersionRequestFilter();
        HttpRequest mockHttpRequest = mock(HttpRequest.class);
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(httpHeaders.getHeaderString(anyString())).then(invocation -> httpHeaders.getRequestHeaders().getFirst((String) invocation.getArguments()[0]));
        when(mockHttpRequest.getHttpHeaders()).thenReturn(httpHeaders);
        when(mockHttpRequest.getMutableHeaders()).thenReturn(new MultivaluedHashMap<>());
        requestContext = new PreMatchContainerRequestContext(mockHttpRequest);
    }

    @Test
    public void shouldDoNothingIfAPIVersionHeaderNotSet() throws Exception {
        requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "application/json");
        requestFilter.filter(requestContext);
        assertThat(requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
    }

    @Test
    public void shouldAddVersionToNullContentType() throws Exception {
        requestContext.getHeaders().putSingle("API-Version", "v1");
        requestFilter.filter(requestContext);
        assertThat(requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/v1+json");
    }

    @Test
    public void shouldAddVersionToApplicationJsonContentType() throws Exception {
        requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "application/json");
        requestContext.getHeaders().putSingle("API-Version", "v1");
        requestFilter.filter(requestContext);
        assertThat(requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/v1+json");
    }

    @Test
    public void shouldAddVersionToCustomApplicationJsonContentType() throws Exception {
        requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
        requestContext.getHeaders().putSingle("API-Version", "v1");
        requestFilter.filter(requestContext);
        assertThat(requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json-patch.v1+json");
    }

    @Test
    public void shouldAddVersionToApplicationWildcardJson() throws Exception {
        requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "application/*+json");
        requestContext.getHeaders().putSingle("API-Version", "v1");
        requestFilter.filter(requestContext);
        assertThat(requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/*.v1+json");
    }

    @Test
    public void shouldReturnBadResponseIfInvalidVersionFormat() throws Exception {
        requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, "application/json");
        requestContext.getHeaders().putSingle("API-Version", "123");
        try {
            requestFilter.filter(requestContext);
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            assertThat(e).hasMessage("Invalid API version format, expected v[0-9] 123");
            assertThat(e.getResponse().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        }

    }

}