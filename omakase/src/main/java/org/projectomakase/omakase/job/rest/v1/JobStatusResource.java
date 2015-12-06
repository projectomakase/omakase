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
import org.projectomakase.omakase.rest.model.v1.ResourceStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.projectomakase.omakase.time.DateTimeFormatters;
import org.jboss.resteasy.annotations.GZIP;

import javax.json.Json;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * JAX-RS Subresource for /jobs/{jobId}/status
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class JobStatusResource {

    private final JobManager jobManager;

    public JobStatusResource(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @GET
    @GZIP
    public Response getJobStatus(@PathParam("jobId") String jobId) {
        Job job = jobManager.getJob(jobId).orElseThrow(NotFoundException::new);
        return Response.ok(Json.createObjectBuilder().add("current", job.getStatus().name()).add("timestamp", getTimestampAsString(job)).build()).build();
    }

    @PUT
    public Response updateJobStatus(@PathParam("jobId") String jobId, ResourceStatusModel resourceStatusModel) {
        jobManager.updateJobStatus(jobId, resourceStatusModel.getCurrent());
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    private static String getTimestampAsString(Job job) {
        return DateTimeFormatters.getDefaultZonedDateTimeFormatter().format(ZonedDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault()));
    }
}
