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
package org.projectomakase.omakase.rest.response;

import org.projectomakase.omakase.Tests;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.rest.model.v1.Href;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class ResponsesTest {

    @Test
    public void shouldReturnAcceptedResponseWithStatusEntity() throws Exception {
        UriInfo uriInfo = Tests.mockUriInfo(Tests.mockUriBuilder(new URI("http://localhost:8080/omakase/api/status/123")));
        Response response = Responses.acceptedResponseWithStatusEntity(uriInfo, "123", "test message");
        assertThat(response.getStatus()).isEqualTo(202);
        Assertions.assertThat((ResponseStatusModel) response.getEntity()).isEqualToIgnoringNullFields(new ResponseStatusModel(ResponseStatusValue.OK, "test message"));
        Map<String, Href> links = ((ResponseStatusModel) response.getEntity()).getLinks();
        assertThat(links).hasSize(1);
        Map.Entry<String, Href> entry = links.entrySet().stream().findFirst().get();
        assertThat(entry.getKey()).isEqualTo("status");
        assertThat(entry.getValue()).isEqualToComparingFieldByField(new Href("http://localhost:8080/omakase/api/status/123"));
    }

    @Test
    public void shouldReturnSeeOtherResponseWithStatusURI() throws Exception {
        UriInfo uriInfo = Tests.mockUriInfo(Tests.mockUriBuilder(new URI("http://localhost:8080/omakase/api/status/123")));
        Response response = Responses.seeOtherResponseStatus(uriInfo, "123");
        assertThat(response.getStatus()).isEqualTo(303);
        assertThat(response.getLocation()).isEqualTo(new URI("http://localhost:8080/omakase/api/status/123"));
    }

    @Test
    public void shouldReturnErrorResponseWithStatusEntity() {
        Response originalResponse = Response.serverError().build();
        Response errorResponse = Responses.errorResponse(originalResponse, "test message");
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        ResponseStatusModel responseStatusModel = (ResponseStatusModel) errorResponse.getEntity();
        assertThat(responseStatusModel).isEqualToIgnoringNullFields(new ResponseStatusModel(ResponseStatusValue.ERROR, "test message"));
    }

    @Test
    public void shouldReturnErrorResponseWithStatusEntityAndException() {
        Response originalResponse = Response.serverError().build();
        Response errorResponse = Responses.errorResponse(originalResponse, "test message", new OmakaseRuntimeException("test"), true);
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        ResponseStatusModel responseStatusModel = (ResponseStatusModel) errorResponse.getEntity();
        assertThat(responseStatusModel).isEqualToComparingFieldByField(new ResponseStatusModel(ResponseStatusValue.ERROR, "test message", "org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException: test"));
    }

    @Test
    public void shouldReturnErrorResponseWithStatusEntityAIgnoringException() {
        Response originalResponse = Response.serverError().build();
        Response errorResponse = Responses.errorResponse(originalResponse, "test message", new OmakaseRuntimeException("test"), false);
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        ResponseStatusModel responseStatusModel = (ResponseStatusModel) errorResponse.getEntity();
        assertThat(responseStatusModel).isEqualToComparingFieldByField(new ResponseStatusModel(ResponseStatusValue.ERROR, "test message", null));
    }

}