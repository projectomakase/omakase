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
package org.projectomakase.omakase.job;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.event.DeleteAsset;
import org.projectomakase.omakase.event.DeleteVariant;
import org.projectomakase.omakase.event.DeleteVariantFromRepository;
import org.projectomakase.omakase.event.SubmitJob;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class JobEventHandlerTest {

    private JobEventHandler jobEventHandler;
    private JobDAO jobDAO;

    @Before
    public void before() {
        jobDAO = Mockito.mock(JobDAO.class);
        jobEventHandler = new JobEventHandler();
        jobEventHandler.jobDAO = jobDAO;

    }

    @Test
    public void shouldThrowExceptionIfAssetHasQueuedJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.QUEUED)))).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active jobs are associated with asset " + deleteAsset.getAssetId());
        }

    }

    @Test
    public void shouldThrowExceptionIfAssetHasExecutingJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.EXECUTING)))).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active jobs are associated with asset " + deleteAsset.getAssetId());
        }

    }

    @Test
    public void shouldNotThrowExceptionIfAssetHasNoUnsubmittedJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.UNSUBMITTED)))).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.UNSUBMITTED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfAssetHasNoCanceledJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.CANCELED)))).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.CANCELED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfAssetHasNoCompletedJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.COMPLETED)))).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.COMPLETED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfAssetHasNoFailedJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.FAILED)))).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.FAILED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfAssetHasNoJobs() {
        DeleteAsset deleteAsset = new DeleteAsset(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of()).when(jobDAO).findJobsForAsset(Mockito.anyString());
            jobEventHandler.handleDeleteAsset(deleteAsset);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception", e);
        }

    }

    @Test
    public void shouldThrowExceptionIfVariantHasQueuedJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.QUEUED)))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active jobs are associated with variant " + deleteVariant.getVariantId());
        }

    }

    @Test
    public void shouldThrowExceptionIfVariantHasExecutingJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.EXECUTING)))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active jobs are associated with variant " + deleteVariant.getVariantId());
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasUnsubmittedJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.UNSUBMITTED)))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.UNSUBMITTED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasCanceledJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.CANCELED)))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.CANCELED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasUCompletedJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.COMPLETED)))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.COMPLETED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasFailedJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.FAILED)))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.FAILED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasNoJobs() {
        DeleteVariant deleteVariant = new DeleteVariant(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of()).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleDeleteVariant(deleteVariant);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception", e);
        }
    }

    @Test
    public void shouldThrowExceptionIfVariantAndRepositoryHasQueuedJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.QUEUED)))).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage(
                    "One or more active jobs are associated with variant " + deleteVariantFromRepository.getVariantId() + " and repository " + deleteVariantFromRepository.getRepositoryId());
        }

    }

    @Test
    public void shouldThrowExceptionIfVariantAndRepositoryHasExecutingJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.EXECUTING)))).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active jobs are associated with variant " + deleteVariantFromRepository.getVariantId() + " and repository " + deleteVariantFromRepository.getRepositoryId());
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantAndRepositoryHaveNoUnsubmittedJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.UNSUBMITTED)))).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.UNSUBMITTED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantAndRepositoryHaveNoCanceledJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.CANCELED)))).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.CANCELED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantAndRepositoryHaveNoCompletedJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.COMPLETED)))).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.COMPLETED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantAndRepositoryHaveNoFailedJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> j.setStatus(JobStatus.FAILED)))).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.FAILED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantAndRepositoryHaveNoJobs() {
        DeleteVariantFromRepository deleteVariantFromRepository = new DeleteVariantFromRepository(new IdGenerator().getId(), "test");
        try {
            Mockito.doReturn(ImmutableList.of()).when(jobDAO).findJobsForVariantAndRepository(Mockito.anyString(), Mockito.anyString());
            jobEventHandler.handleDeleteVariantFromRepository(deleteVariantFromRepository);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception", e);
        }
    }

    @Test
    public void shouldThrowExceptionIfVariantHasQueuedDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> {j.setStatus(JobStatus.QUEUED); j.setJobType(JobType.DELETE);}))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active delete jobs are associated with variant " + submitJob.getVariantId());
        }

    }

    @Test
    public void shouldThrowExceptionIfVariantHasExecutingDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> {j.setStatus(JobStatus.EXECUTING); j.setJobType(JobType.DELETE);}))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("One or more active delete jobs are associated with variant " + submitJob.getVariantId());
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasUnsubmittedDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> {j.setStatus(JobStatus.UNSUBMITTED); j.setJobType(JobType.DELETE);}))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.UNSUBMITTED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasCanceledDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> {j.setStatus(JobStatus.CANCELED); j.setJobType(JobType.DELETE);}))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.CANCELED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasUCompletedDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> {j.setStatus(JobStatus.COMPLETED); j.setJobType(JobType.DELETE);}))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.COMPLETED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasFailedDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of(Job.Builder.build(j -> {j.setStatus(JobStatus.FAILED); j.setJobType(JobType.DELETE);}))).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception for job status " + JobStatus.FAILED, e);
        }

    }

    @Test
    public void shouldNotThrowExceptionIfVariantHasNoDeleteJobs() {
        SubmitJob submitJob = new SubmitJob(new IdGenerator().getId());
        try {
            Mockito.doReturn(ImmutableList.of()).when(jobDAO).findJobsForVariant(Mockito.anyString());
            jobEventHandler.handleSubmitJob(submitJob);
        } catch (NotUpdateableException e) {
            fail("Unexpected exception", e);
        }
    }


}