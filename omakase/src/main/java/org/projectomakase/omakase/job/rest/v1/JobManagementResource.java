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
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.rest.v1.converter.JobQuerySearchConverter;
import org.projectomakase.omakase.job.rest.v1.model.JobConfigurationModel;
import org.projectomakase.omakase.job.rest.v1.model.JobModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.converter.v1.MessageQuerySearchConverter;
import org.projectomakase.omakase.rest.model.v1.MessageModel;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

/**
 * Job Management JAX-RS Resource implementation. Exposes REST API operations for managing jobs.
 *
 * @author Richard Lucas
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class JobManagementResource {

    @Inject
    RepresentationConverter<JobModel, Job> jobRepresentationConverter;
    @Inject
    RepresentationConverter<JobConfigurationModel, JobConfiguration> jobConfigurationRepresentationConverter;
    @Inject
    JobQuerySearchConverter querySearchConverter;
    @Inject
    RepresentationConverter<MessageModel, Message> messageRepresentationConverter;
    @Inject
    MessageQuerySearchConverter messageQuerySearchConverter;
    @Inject
    JobManager jobManager;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    @Path("/jobs")
    public JobsResource getJobsResource() {
        return new JobsResource(jobManager, jobRepresentationConverter, querySearchConverter, uriInfo);
    }

    @Path("/jobs/{jobId}")
    public JobResource getJobResource() {
        return new JobResource(jobManager, jobRepresentationConverter, uriInfo, request);
    }

    @Path("/jobs/{jobId}/configuration")
    public JobConfigurationResource getJobConfigurationResource() {
        return new JobConfigurationResource(jobManager, jobConfigurationRepresentationConverter, uriInfo);
    }

    @Path("/jobs/{jobId}/status")
    public JobStatusResource getJobStatusResource() {
        return new JobStatusResource(jobManager);
    }

    @Path("/jobs/{jobId}/messages")
    public JobMessagesResource getJobMessagesResource() {
        return new JobMessagesResource(jobManager, messageRepresentationConverter, messageQuerySearchConverter, uriInfo);
    }

}
