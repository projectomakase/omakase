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
import org.projectomakase.omakase.content.rest.v1.model.RepositoryModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.response.Responses;
import org.jboss.resteasy.annotations.GZIP;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * JAX-RS Subresource for /assets/{assetId}/variants/{variantId}/repositories/{repositoryId}
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class RepositoryResource {

    private final ContentManager contentManager;
    private final RepresentationConverter<RepositoryModel, VariantRepository> repositoryRepresentationConverter;
    private final UriInfo uriInfo;
    private final Request request;

    public RepositoryResource(ContentManager contentManager, RepresentationConverter<RepositoryModel, VariantRepository> repositoryRepresentationConverter, UriInfo uriInfo, Request request) {
        this.contentManager = contentManager;
        this.repositoryRepresentationConverter = repositoryRepresentationConverter;
        this.uriInfo = uriInfo;
        this.request = request;
    }

    @GET
    @GZIP
    public Response getRepository(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId, @PathParam("repositoryId") String repositoryId) {
        VariantRepository repository = contentManager.getVariantRepository(assetId, variantId, repositoryId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(repository.getCreated().getTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            RepositoryModel repositoryModel = repositoryRepresentationConverter.from(uriInfo, repository);
            return Response.ok(repositoryModel).tag(etag).build();
        }
    }

    @DELETE
    public Response deleteVariantFromRepository(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId, @PathParam("repositoryId") String repositoryId) {
        contentManager.getVariant(assetId, variantId).orElseThrow(NotFoundException::new);
        String jobId = contentManager.deleteVariantFromRepository(assetId, variantId, repositoryId);
        return Responses.acceptedResponseWithStatusEntity(uriInfo, jobId, "Variant " + variantId + " is being deleted from repository " + repositoryId);
    }
}