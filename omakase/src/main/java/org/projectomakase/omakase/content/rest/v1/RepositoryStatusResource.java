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
package org.projectomakase.omakase.content.rest.v1;

import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.time.DateTimeFormatters;
import org.jboss.resteasy.annotations.GZIP;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * JAX-RS Subresource for /assets/{assetId}/variants/{variantId}/repositories/{repositoryId}/status
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class RepositoryStatusResource {

    private final ContentManager contentManager;

    public RepositoryStatusResource(ContentManager contentManager) {
        this.contentManager = contentManager;
    }

    @GET
    @GZIP
    public Response getRepositoryStatus(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId, @PathParam("repositoryId") String repositoryId) {
        return contentManager.getVariantRepository(assetId, variantId, repositoryId).map(RepositoryStatusResource::getResponse).orElseThrow(NotFoundException::new);
    }

    private static Response getResponse(VariantRepository variantRepository) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("current", "AVAILABLE");
        jsonObjectBuilder.add("timestamp", getTimestampAsString(variantRepository));
        return Response.ok(jsonObjectBuilder.build()).build();
    }
    private static String getTimestampAsString(VariantRepository variantRepository) {
        return DateTimeFormatters.getDefaultZonedDateTimeFormatter().format(ZonedDateTime.ofInstant(variantRepository.getCreated().toInstant(), ZoneId.systemDefault()));
    }
}
