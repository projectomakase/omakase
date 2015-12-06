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
package org.projectomakase.omakase.rest.status.v1;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.model.v1.Href;
import org.projectomakase.omakase.rest.status.v1.model.StatusModel;
import org.jboss.resteasy.annotations.GZIP;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Status JAX-RS Resource implementation. Exposes REST API operations for retrieving temporary status resources.
 * <p>
 * Status resources are temporary and eventually will no longer be in available.
 * </p>
 * <p>
 * The current implementation assumes that status resources are backed by Omakase Jobs and the status id maps to a job id. When a status resource is requested the job for that status is received and
 * used to build the response.
 * </p>
 *
 * @author Richard Lucas
 */
@Path("status")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class StatusResource {

    @Inject
    JobManager jobManager;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    @GET
    @GZIP
    @Path("/{statusId}")
    public Response getStatus(@PathParam("statusId") String statusId) {
        Job job = jobManager.getJob(statusId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(job.getStatusTimestamp().getTime());
        return Optional.ofNullable(request.evaluatePreconditions(etag)).orElse(Response.ok(convertJobToStatusModel(job))).tag(etag).build();
    }

    private StatusModel convertJobToStatusModel(Job job) {
        return new StatusModel(job.getStatus().toString(), ZonedDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault()), ImmutableMap.of("job", getJobHref(job)));
    }

    private Href getJobHref(Job job) {
        return new Href(uriInfo.getBaseUriBuilder().path("job").path(job.getId()).build().toString());
    }
}
