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
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.job.rest.v1.interceptor.PutJobConfig;
import org.projectomakase.omakase.job.rest.v1.model.JobConfigurationModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.jboss.resteasy.annotations.GZIP;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * JAX-RS Subresource for /jobs/{jobId}/configuration
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class JobConfigurationResource {

    private final JobManager jobManager;
    private final RepresentationConverter<JobConfigurationModel, JobConfiguration> jobConfigurationRepresentationConverter;
    private final UriInfo uriInfo;

    public JobConfigurationResource(JobManager jobManager, RepresentationConverter<JobConfigurationModel, JobConfiguration> jobConfigurationRepresentationConverter, UriInfo uriInfo) {
        this.jobManager = jobManager;
        this.jobConfigurationRepresentationConverter = jobConfigurationRepresentationConverter;
        this.uriInfo = uriInfo;
    }

    @GET
    @GZIP
    public Response getJobConfiguration(@PathParam("jobId") String jobId) {
        Job job = jobManager.getJob(jobId).orElseThrow(NotFoundException::new);
        JobConfiguration jobConfiguration = job.getJobConfiguration();
        JobConfigurationModel jobConfigurationModel = jobConfigurationRepresentationConverter.from(uriInfo, jobConfiguration);
        return Response.ok(jobConfigurationModel).build();
    }

    @PUT
    @PutJobConfig
    public Response updateJobConfiguration(@PathParam("jobId") String jobId, JobConfigurationModel jobConfigurationModel) {
        Job job = jobManager.getJob(jobId).orElseThrow(NotFoundException::new);
        jobManager.updateJobConfiguration(jobId, jobConfigurationRepresentationConverter.update(uriInfo, jobConfigurationModel, job.getJobConfiguration()));
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }
}
