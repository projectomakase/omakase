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

import org.projectomakase.omakase.Tests;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.model.v1.Href;
import org.projectomakase.omakase.rest.status.v1.model.StatusModel;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class StatusResourceTest {

    private StatusResource statusResource;
    private JobManager mockJobManager;
    private Request mockRequest;

    @Before
    public void before() throws Exception {
        mockJobManager = mock(JobManager.class);
        mockRequest = mock(Request.class);

        statusResource = new StatusResource();
        statusResource.jobManager = mockJobManager;
        statusResource.request = mockRequest;
        statusResource.uriInfo = Tests.mockUriInfo(Tests.mockUriBuilder(new URI("http://localhost:8080/omakase/api/jobs/123")));
    }

    @Test
    public void shouldGetStatusResponse() {
        ZonedDateTime now = ZonedDateTime.now();
        doReturn(Optional.of(Job.Builder.build(job -> {
            job.setId("123");
            job.setStatus(JobStatus.EXECUTING);
            job.setStatusTimestamp(Date.from(now.toInstant()));
        }))).when(mockJobManager).getJob("123");
        Response response = statusResource.getStatus("123");
        assertThat(response.getStatus()).isEqualTo(200);
        StatusModel statusModel = (StatusModel) response.getEntity();
        assertThat(statusModel).isEqualToIgnoringGivenFields(new StatusModel("EXECUTING", now, null), "links");

        Assertions.assertThat(statusModel.getLinks()).hasSize(1);
        Map.Entry<String, Href> entry = statusModel.getLinks().entrySet().stream().findFirst().get();
        assertThat(entry.getKey()).isEqualTo("job");
        assertThat(entry.getValue()).isEqualToComparingFieldByField(new Href("http://localhost:8080/omakase/api/jobs/123"));
    }

    @Test
    public void shouldGetStatusResponseEtag() {
        ZonedDateTime now = ZonedDateTime.now();
        doReturn(Optional.of(Job.Builder.build(job -> {
            job.setId("123");
            job.setStatus(JobStatus.EXECUTING);
            job.setStatusTimestamp(Date.from(now.toInstant()));
        }))).when(mockJobManager).getJob("123");

        EntityTag entityTag = EntityTagGenerator.entityTagFromLong(now.toInstant().toEpochMilli());

                doReturn(Response.notModified(entityTag)).when(mockRequest).evaluatePreconditions(any(EntityTag.class));
        Response response = statusResource.getStatus("123");
        assertThat(response.getStatus()).isEqualTo(304);
        assertThat(response.getEntity()).isNull();
        assertThat(response.getEntityTag()).isEqualToComparingFieldByField(entityTag);
    }

}