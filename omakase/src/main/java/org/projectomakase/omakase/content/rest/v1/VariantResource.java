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

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.rest.v1.model.VariantModel;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobSearchBuilder;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.projectomakase.omakase.rest.patch.PATCH;
import org.projectomakase.omakase.rest.response.Responses;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
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
import java.util.Optional;

/**
 * JAX-RS Subresource for /assets/{assetId}/variants/{variantId}
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class VariantResource {

    private final ContentManager contentManager;
    private final JobManager jobManager;
    private final RepresentationConverter<VariantModel, Variant> variantRepresentationConverter;
    private final UriInfo uriInfo;
    private final Request request;

    public VariantResource(ContentManager contentManager, JobManager jobManager, RepresentationConverter<VariantModel, Variant> variantRepresentationConverter, UriInfo uriInfo, Request request) {
        this.contentManager = contentManager;
        this.jobManager = jobManager;
        this.variantRepresentationConverter = variantRepresentationConverter;
        this.uriInfo = uriInfo;
        this.request = request;
    }

    @GET
    @GZIP
    public Response getAssetVariant(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId) {

        Optional<Variant> variant = contentManager.getVariant(assetId, variantId);

        if (variant.isPresent()) {
            EntityTag etag = EntityTagGenerator.entityTagFromLong(variant.get().getLastModified().getTime());
            return Optional.ofNullable(request.evaluatePreconditions(etag)).orElse(Response.ok(variantRepresentationConverter.from(uriInfo, variant.get()))).tag(etag).build();
        } else {
            return findDeleteJobForVariant(variantId).map(job -> Response.seeOther(uriInfo.getBaseUriBuilder().path("status").path(job.getId()).build())).orElseThrow(NotFoundException::new).build();
        }
    }

    @DELETE
    public Response deleteAssetVariant(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId) {
        return contentManager.deleteVariant(assetId, variantId).map(jobId -> Responses.acceptedResponseWithStatusEntity(uriInfo, jobId, "Variant " + variantId + " is being deleted"))
                .orElse(Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build());
    }

    @PUT
    public Response updateAssetVariant(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId, VariantModel variantModel) {
        contentManager.updateVariant(variantRepresentationConverter.update(uriInfo, variantModel, contentManager.getVariant(assetId, variantId).orElseThrow(NotFoundException::new)));
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @PATCH
    @Consumes({"application/json-patch+json", "application/json-patch.v1+json"})
    public Response partialUpdateAssetVariant(@PathParam("assetId") String assetId, @PathParam("variantId") String variantId, VariantModel variantModel) {
        return updateAssetVariant(assetId, variantId, variantModel);
    }

    private Optional<Job> findDeleteJobForVariant(String variantId) {
        return jobManager.findJobs(
                new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.TYPE, Operator.EQ, "DELETE"),
                        new SearchCondition(Job.EXTERNAL_IDS, Operator.EQ, variantId),
                        new SearchCondition(Job.STATUS, Operator.EQ, "QUEUED, EXECUTING"))).build())
                .getRecords().stream().findFirst();
    }

}
