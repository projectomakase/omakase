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
package org.projectomakase.omakase.job.rest.v1;

import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.rest.v1.model.JobModel;
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
 * JAX-RS Subresource for /jobs/{jobId}
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class JobResource {

    private final JobManager jobManager;
    private final RepresentationConverter<JobModel, Job> jobRepresentationConverter;
    private final UriInfo uriInfo;
    private final Request request;

    public JobResource(JobManager jobManager, RepresentationConverter<JobModel, Job> jobRepresentationConverter, UriInfo uriInfo, Request request) {
        this.jobManager = jobManager;
        this.jobRepresentationConverter = jobRepresentationConverter;
        this.uriInfo = uriInfo;
        this.request = request;
    }

    @GET
    @GZIP
    public Response getJob(@PathParam("jobId") String jobId) {
        Job job = jobManager.getJob(jobId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(job.getLastModified().getTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            JobModel jobModel = jobRepresentationConverter.from(uriInfo, job);
            return Response.ok(jobModel).tag(etag).build();
        }
    }

    @DELETE
    public Response deleteJob(@PathParam("jobId") String jobId) {
        jobManager.deleteJob(jobId);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @PUT
    public Response updateJob(@PathParam("jobId") String jobId, JobModel jobModel) {
        jobManager.updateJob(jobRepresentationConverter.update(uriInfo, jobModel, jobManager.getJob(jobId).orElseThrow(NotFoundException::new)));
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @PATCH
    @Consumes({"application/json-patch+json", "application/json-patch.v1+json"})
    public Response patchJob(@PathParam("jobId") String jobId, JobModel jobModel) {
        return updateJob(jobId, jobModel);
    }
}
