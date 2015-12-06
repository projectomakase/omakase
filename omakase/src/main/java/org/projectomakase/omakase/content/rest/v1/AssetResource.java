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

import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.rest.v1.model.AssetModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.projectomakase.omakase.rest.patch.PATCH;
import org.jboss.resteasy.annotations.GZIP;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * JAX-RS Subresource for /assets/{assetId}
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class AssetResource {

    private final ContentManager contentManager;
    private final RepresentationConverter<AssetModel, Asset> assetRepresentationConverter;
    private final UriInfo uriInfo;
    private final Request request;

    public AssetResource(ContentManager contentManager, RepresentationConverter<AssetModel, Asset> assetRepresentationConverter, UriInfo uriInfo, Request request) {
        this.contentManager = contentManager;
        this.assetRepresentationConverter = assetRepresentationConverter;
        this.uriInfo = uriInfo;
        this.request = request;
    }

    @GET
    @GZIP
    public Response getAssetById(@PathParam("assetId") String assetId) {

        Asset asset = contentManager.getAsset(assetId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(asset.getLastModified().getTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            AssetModel assetModel = assetRepresentationConverter.from(uriInfo, asset);
            return Response.ok(assetModel).tag(etag).build();
        }
    }

    @DELETE
    public Response deleteAsset(@PathParam("assetId") String assetId) {
        contentManager.deleteAsset(assetId);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @PUT
    public Response updateAsset(@PathParam("assetId") String assetId, AssetModel assetModel) {
        contentManager.updateAsset(assetRepresentationConverter.update(uriInfo, assetModel, contentManager.getAsset(assetId).orElseThrow(NotFoundException::new)));
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @PATCH
    @Consumes({"application/json-patch+json", "application/json-patch.v1+json"})
    public Response partialUpdateAsset(@PathParam("assetId") String assetId, AssetModel assetModel) {
        return updateAsset(assetId, assetModel);
    }
}
