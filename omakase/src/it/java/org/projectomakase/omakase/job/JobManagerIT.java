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
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.job.configuration.DeleteVariantJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageSearchBuilder;
import org.projectomakase.omakase.job.message.MessageType;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * @author Richard Lucas
 */
@RunWith(Arquillian.class)
public class JobManagerIT {

    @Inject
    JobManager jobManager;
    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    IdGenerator idGenerator;
    @Inject
    JobDAO jobDAO;
    @Inject
    TaskManager taskManager;
    @Inject
    IntegrationTests integrationTests;

    private Variant variant;
    private Repository configuredRepository;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() throws Exception {
        TestRunner.runAsUser("admin", "password", this::setup);
        integrationTests.destroyJCRSession();
    }

    @After
    public void after() {
        integrationTests.destroyJCRSession();
        TestRunner.runAsUser("admin", "password", this::cleanup);
    }

    @Test
    public void shouldFailToCreateJobNotAuthorized() throws Exception {
        TestRunner.runAsUser("reader", "password", () -> {
            try {
                Job job = getJob();
                jobManager.createJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotAuthorizedException.class);
            }
        });
    }

    @Test
    public void shouldFailToCreateJobWithoutType() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                Job job = Job.Builder.build(j -> j.setJobConfiguration(getJobConfiguration()));
                jobManager.createJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(InvalidPropertyException.class);
                assertThat(e.getCause()).hasMessage("Type can not be null");
            }
        });
    }

    @Test
    public void shouldFailToCreateJobInvalidStatus() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                Job job = Job.Builder.build(j -> {
                    j.setJobType(getJobType());
                    j.setStatus(JobStatus.EXECUTING);
                    j.setJobConfiguration(getJobConfiguration());
                });
                jobManager.createJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(InvalidPropertyException.class);
                assertThat(e.getCause()).hasMessage("Invalid Job Status " + JobStatus.EXECUTING + ", the value must be UNSUBMITTED or QUEUED");
            }
        });
    }

    @Test
    public void shouldFailToCreateJobInvalidPriority() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                Job job = Job.Builder.build(j -> {
                    j.setJobType(getJobType());
                    j.setPriority(23);
                    j.setJobConfiguration(getJobConfiguration());
                });
                jobManager.createJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(InvalidPropertyException.class);
                assertThat(e.getCause()).hasMessage("Invalid priority 23, the value must be between 1 and 10");
            }
        });
    }

    @Test
    public void shouldFailToCreateJobNoConfiguration() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                Job job = Job.Builder.build(j -> j.setJobType(getJobType()));
                jobManager.createJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(InvalidPropertyException.class);
                assertThat(e.getCause()).hasMessage("Configuration can not be null");
            }
        });
    }

    @Test
    public void shouldFailToCreateAndSubmitJobIfVariantHasActiveDeleteJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job deleteJob = new Job();
            deleteJob.setJobType(JobType.DELETE);
            deleteJob.setStatus(JobStatus.QUEUED);
            deleteJob.setJobConfiguration(new DeleteVariantJobConfiguration(variant.getId(), ImmutableList.of(configuredRepository.getId())));
            jobManager.createJob(deleteJob);

            Job job = getJob();
            job.setStatus(JobStatus.QUEUED);
            assertThatThrownBy(() -> jobManager.createJob(job))
                    .isExactlyInstanceOf(EJBException.class)
                    .hasCauseExactlyInstanceOf(NotUpdateableException.class)
                    .hasMessageEndingWith("One or more active delete jobs are associated with variant " + variant.getId());
        });
    }

    @Test
    public void shouldFindJobs() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 15; i++) {
                Job job = getJob();
                jobManager.createJob(job);
            }
            Search search = new JobSearchBuilder().build();
            assertThat(jobManager.findJobs(search).getRecords()).hasSize(10);
            Search onlyCountSearch = new JobSearchBuilder().onlyCount(true).build();
            assertThat(jobManager.findJobs(onlyCountSearch).getTotalRecords()).isEqualTo(15);

        });
    }

    @Test
    public void shouldGetJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            Job createdJob = jobManager.createJob(job);
            Job retrievedJob = jobManager.getJob(createdJob.getId()).get();
            assertThat(retrievedJob).isEqualToIgnoringGivenFields(createdJob, "jobConfiguration");
            assertJobConfiguration(createdJob, retrievedJob);
        });
    }


    @Test
    public void shouldReturnEmptyOptionalForJobIdThatDoesNotExist() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> assertThat(jobManager.getJob(idGenerator.getId()).isPresent()).isFalse());
    }

    @Test
    public void shouldUpdateJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = Job.Builder.build(j -> {
                j.setJobName("test-job");
                j.setExternalIds(ImmutableList.of("a", "b"));
                j.setJobType(getJobType());
                j.setStatus(JobStatus.UNSUBMITTED);
                j.setPriority(1);
                j.setJobConfiguration(getJobConfiguration());
            });

            Job createdJob = jobManager.createJob(job);
            createdJob.setJobName("test-job2");
            createdJob.setExternalIds(ImmutableList.of("c", "d"));
            createdJob.setPriority(5);

            Job updatedJob = jobManager.updateJob(createdJob);
            assertThat(updatedJob.getId()).isNotNull();
            assertThat(updatedJob.getJobName()).isEqualTo("test-job2");
            assertThat(updatedJob.getExternalIds()).containsExactly("c", "d");
            assertThat(updatedJob.getJobType()).isEqualTo(getJobType());
            assertThat(updatedJob.getStatus()).isEqualTo(JobStatus.UNSUBMITTED);
            assertThat(updatedJob.getStatusTimestamp()).isNotNull();
            assertThat(updatedJob.getPriority()).isEqualTo(5);
            assertThatJobHasCreatedAndLastModified(updatedJob, "admin");
            assertThatDateAIsAfterDateB(updatedJob.getLastModified(), createdJob.getLastModified());
        });
    }

    @Test
    public void shouldFailToUpdateJobNoId() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setStatus(JobStatus.EXECUTING);
            try {
                jobManager.updateJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("The new status does not match the current status");
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobInvalidState() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setStatus(JobStatus.EXECUTING);
            jobDAO.update(job);
            try {
                jobManager.updateJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Unable to update a job with status EXECUTING only UNSUBMITTED an FAILED jobs can be updated");
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobWithDifferentType() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setJobType(JobType.TRANSFORMATION);
            try {
                jobManager.updateJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Changing the job type is not supported");
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobInvalidPriority() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setPriority(23);
            try {
                jobManager.updateJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(InvalidPropertyException.class);
                assertThat(e.getCause()).hasMessage("Invalid priority 23, the value must be between 1 and 10");
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobStatusViaJobUpdate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setStatus(JobStatus.EXECUTING);
            try {
                jobManager.updateJob(job);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("The new status does not match the current status");
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobStatusInvalidState() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            try {
                jobManager.updateJobStatus(job.getId(), JobStatus.EXECUTING);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Job status can not be updated to " + JobStatus.EXECUTING);
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobStatusToCancelled() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setStatus(JobStatus.QUEUED);
            job = jobDAO.update(job);
            try {
                jobManager.updateJobStatus(job.getId(), JobStatus.CANCELED);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Job status can not be updated to " + JobStatus.CANCELED);
            }
        });
    }

    @Test
    public void shouldFailToUpdateJobStatusToQueuedIfVariantHasActiveDeleteJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job deleteJob = new Job();
            deleteJob.setJobType(JobType.DELETE);
            deleteJob.setStatus(JobStatus.QUEUED);
            deleteJob.setJobConfiguration(new DeleteVariantJobConfiguration(variant.getId(), ImmutableList.of(configuredRepository.getId())));
            jobManager.createJob(deleteJob);

            Job job = jobManager.createJob(getJob());
            assertThatThrownBy(() -> jobManager.updateJobStatus(job.getId(), JobStatus.QUEUED))
                    .isExactlyInstanceOf(EJBException.class)
                    .hasCauseExactlyInstanceOf(NotUpdateableException.class)
                    .hasMessageEndingWith("One or more active delete jobs are associated with variant " + variant.getId());
        });
    }

    @Test
    public void shouldDeleteJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            Job createdJob = jobManager.createJob(job);
            jobManager.deleteJob(createdJob.getId());
            assertThat(jobManager.getJob(createdJob.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldFailToDeleteQueuedJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setStatus(JobStatus.QUEUED);
            job = jobDAO.update(job);
            try {
                jobManager.deleteJob(job.getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Unable to delete job. Job " + job.getId() + " has a status of " + job.getStatus());
            }
        });
    }

    @Test
    public void shouldFailToDeleteExecutingJob() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            job.setStatus(JobStatus.EXECUTING);
            job = jobDAO.update(job);
            try {
                jobManager.deleteJob(job.getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Unable to delete job. Job " + job.getId() + " has a status of " + job.getStatus());
            }
        });
    }

    @Test
    public void shouldFindMessagesForJob() {
        TestRunner.runAsUser("admin", "password", () -> {

            Job job = jobManager.createJob(getJob());

            List<String> tasks = IntStream.range(0, 5).mapToObj(i -> {
                Task task = createTransferTask(job);
                taskManager.addMessageToTask(task.getId(), new Message("info message " + i, MessageType.INFO));
                taskManager.addMessageToTask(task.getId(), new Message("error message " + i, MessageType.ERROR));
                return task.getId();
            }).collect(ImmutableListCollector.toImmutableList());

            jobManager.addMessageToJob(job.getId(), new Message("job message", MessageType.INFO));

            List<Message> expectedTaskMessages =
                    IntStream.of(4, 3, 2, 1, 0).mapToObj(i -> ImmutableList.of(new Message("error message " + i, MessageType.ERROR), new Message("info message " + i, MessageType.INFO)))
                            .collect(ImmutableListsCollector.toImmutableList());
            ImmutableList.Builder<Message> messageBuilder = ImmutableList.builder();
            List<Message> expectedMessages = messageBuilder.add(new Message("job message", MessageType.INFO)).addAll(expectedTaskMessages).build();

            List<Message> actualMessages = jobManager.findJobMessages(job.getId(), new MessageSearchBuilder().count(-1).build()).getRecords();
            assertThat(actualMessages).hasSize(11).usingElementComparatorOnFields("messageValue", "messageType").containsExactlyElementsOf(expectedMessages);
        });
    }

    private void setup() {
        cleanup();
        Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of()));
        variant = contentManager.createVariant(asset.getId(), new Variant("test", ImmutableList.of()));
        configuredRepository = repositoryManager.createRepository(new Repository("configured", "configured", "FILE"));
        repositoryManager.updateRepositoryConfiguration(configuredRepository.getId(), new FileRepositoryConfiguration("/test"));
        Repository unconfiguredRepository = repositoryManager.createRepository(new Repository("un-configured", "un-configured", "FILE"));
    }

    private void cleanup() {
        integrationTests.cleanup();
        variant = null;
        configuredRepository = null;
    }

    private JobType getJobType() {
        return JobType.INGEST;
    }

    private Job getJob() {
        return Job.Builder.build(j -> {
            j.setJobType(JobType.INGEST);
            j.setJobConfiguration(getJobConfiguration());
        });
    }

    private JobConfiguration getJobConfiguration() {
        return IngestJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setRepositories(ImmutableList.of(configuredRepository.getId()));
            config.setIngestJobFiles(ImmutableList.of(newIngestJobFile("file:/test")));
        });
    }

    private IngestJobFile newIngestJobFile(String uri) {
        return IngestJobFile.Builder.build(ingestJobFile -> {
            ingestJobFile.setUri(uri);
            ingestJobFile.setHash("a-b-c-d");
            ingestJobFile.setHashAlgorithm("MD5");
        });
    }

    private void assertJobConfiguration(Job createdJob, Job retrievedJob) {
        Assertions.assertThat(retrievedJob.getJobConfiguration()).isEqualToIgnoringGivenFields(createdJob.getJobConfiguration(), "ingestJobFiles");
        assertThat(((IngestJobConfiguration) retrievedJob.getJobConfiguration()).getIngestJobFiles().get(0))
                .isEqualToComparingFieldByField(((IngestJobConfiguration) createdJob.getJobConfiguration()).getIngestJobFiles().get(0));
    }

    private void assertThatJobHasCreatedAndLastModified(Job job, String expectedUser) {
        assertThat(job.getCreated()).isNotNull();
        assertThat(job.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(job.getLastModified()).isNotNull();
        assertThat(job.getLastModifiedBy()).isEqualTo(expectedUser);
    }

    private void assertThatDateAIsAfterDateB(Date dateA, Date dateB) {
        assertThat(ZonedDateTime.ofInstant(dateA.toInstant(), ZoneId.systemDefault()).isAfter(ZonedDateTime.ofInstant(dateB.toInstant(), ZoneId.systemDefault()))).isTrue();
    }

    private Task createTransferTask(Job job) {
        TaskGroup createdTaskGroup = taskManager.createTaskGroup(new TaskGroup(job.getId(), idGenerator.getId(), "test"));
        return Throwables.returnableInstance(() -> taskManager.createTask(createdTaskGroup.getId(), new Task("TRANSFER", "test", 1,
                                                                                                             new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(new URI("file:/test.txt"),
                                                                                                                                                                              new URI("file:/dest.txt"))),
                                                                                                                                           Collections.singletonList("MD5")))));
    }
}