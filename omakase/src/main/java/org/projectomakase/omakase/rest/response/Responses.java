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

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.rest.model.v1.Href;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Richard Lucas
 */
public final class Responses {

    private Responses() {
        // hide implicit constructor
    }

    /**
     * Returns an Accepted {@link Response} with a ResponseStatusModel Entity. The Status Entity includes a message and a link to a a temporary resource that can be used to track the status of the
     * request.
     *
     * @param uriInfo
     *         URI Info
     * @param statusId
     *         the status id that will be used to track the request.
     * @param message
     *         the status message
     * @return an Accepted {@link Response} with a Status Entity.
     */
    public static Response acceptedResponseWithStatusEntity(UriInfo uriInfo, String statusId, String message) {
        ResponseStatusModel responseStatusModel = new ResponseStatusModel(ResponseStatusValue.OK, message);
        responseStatusModel.setLinks(ImmutableMap.of("status", new Href(uriInfo.getBaseUriBuilder().path("status").path(statusId).build().toString())));
        return Response.accepted(responseStatusModel).build();
    }

    /**
     * Returns a Redirect {@link Response} redirecting the caller to the status REST API so they can track the request.
     *
     * @param uriInfo
     *         URI Info
     * @param statusId
     *         the status id that will be used to track the request.
     * @return a Redirect {@link Response} redirecting the caller to the status REST API.
     */
    public static Response seeOtherResponseStatus(UriInfo uriInfo, String statusId) {
        return Response.seeOther(uriInfo.getBaseUriBuilder().path("status").path(statusId).build()).build();
    }

    /**
     * Returns an error response with ResponseStatusModel entity.
     *
     * @param response
     *         the original error response
     * @param message
     *         the error message
     * @return an error response with ResponseStatusModel entity.
     */
    public static Response errorResponse(Response response, String message) {
        return errorResponse(response, message, null, false);
    }

    /**
     * Returns an error response with ResponseStatusModel entity.
     *
     * @param response
     *         the original error response
     * @param message
     *         the error message
     * @param exception
     *         the exception
     * @param includeException
     *         true if the exception should be included in the response otherwise false
     * @return an error response with ResponseStatusModel entity.
     */
    public static Response errorResponse(Response response, String message, Throwable exception, boolean includeException) {
        ResponseStatusModel responseStatusModel;
        if (includeException) {
            responseStatusModel = new ResponseStatusModel(ResponseStatusValue.ERROR, message, exception.toString());
        } else {
            responseStatusModel = new ResponseStatusModel(ResponseStatusValue.ERROR, message);
        }
        return Response.fromResponse(response).header("Warning", responseStatusModel.getMessage()).entity(responseStatusModel).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}
