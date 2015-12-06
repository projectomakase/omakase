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

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.callback.CallbackListener;
import org.projectomakase.omakase.event.SubmitJob;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.job.configuration.JobConfigurationException;
import org.projectomakase.omakase.job.configuration.JobConfigurationValidator;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageDAO;
import org.projectomakase.omakase.job.message.MessageType;
import org.projectomakase.omakase.job.pipeline.JobPipelineBuilder;
import org.projectomakase.omakase.pipeline.PipelineManager;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.logging.Logger;
import org.jcrom.util.NodeFilter;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.search.SearchException;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Java Facade for managing Jobs.
 *
 * @author Richard Lucas
 */
@Stateless
public class JobManager {

    private static final Logger LOGGER = Logger.getLogger(JobManager.class);
    private static final String JOB_CALLBACK_LISTENER = "JOB_CALLBACK_LISTENER";

    @Inject
    @OrganizationNodePath("jobs")
    String jobsNodePath;
    @Inject
    IdGenerator idGenerator;
    @Inject
    JobDAO jobDAO;
    @Inject
    MessageDAO messageDAO;
    @Inject
    JobConfigurationValidator jobConfigurationValidator;
    @Inject
    JobStatusTransition jobStatusTransition;
    @Inject
    PipelineManager pipelineManager;
    @Inject
    JobPipelineBuilder pipelineBuilder;
    @Inject
    Event<SubmitJob> submitJobEvent;

    /**
     * Creates a new job.
     *
     * @param job
     *         the job to create
     * @return the newly created job.
     * @throws InvalidPropertyException
     *         if the job contains invalid properties
     * @throws JobConfigurationException
     *         if the job is incorrectly configured
     */
    public Job createJob(@NotNull final Job job) {
        String id = idGenerator.getId();
        job.setId(id);
        validateJob(job);
        job.getJobConfiguration().validate(jobConfigurationValidator);
        job.setStatusTimestamp(new Date());

        Job createdJob = jobDAO.distributedCreate(getJobPath(Optional.empty()), job, id);
        if (JobStatus.QUEUED.equals(createdJob.getStatus())) {
            LOGGER.debug("Auto-Submitting Job " + createdJob.getId());
            submitJobEvent.fire(new SubmitJob(job.getJobConfiguration().getVariant()));
            createAndExecutePipeline(createdJob);
        }
        return createdJob;
    }

    /**
     * Returns all of the jobs for the caller principles organization that match the given search constraints.
     *
     * @param search
     *         search constraints
     * @return all of the jobs for the caller principles organization that match the given search constraints.
     * @throws SearchException
     *         if the search fails
     */
    public SearchResult<Job> findJobs(@NotNull final Search search) {
        return jobDAO.findNodes(getJobPath(Optional.empty()), search, "omakase:job");
    }


    /**
     * Returns the job for the given job id if it exists otherwise returns an empty Optional.
     *
     * @param jobId
     *         the job id.
     * @return the job for the given job id if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     */
    public Optional<Job> getJob(@NotNull final String jobId) {
        return Optional.ofNullable(jobDAO.get(getJobPath(Optional.of(jobId))));
    }

    /**
     * Updates the job. Only UNSUBMITTED and FAILED jobs can be updated. Only supports updating the following fields:
     * <ul>
     * <li>name</li>
     * <li>external_ids</li>
     * <li>priority</li>
     * </ul>
     *
     * @param job
     *         the job being updated.
     * @return the updated job.
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     * @throws NotFoundException
     *         if a job does not exist for the given job id.
     * @throws InvalidPropertyException
     *         if the job contains invalid properties
     * @throws NotUpdateableException
     *         if the job can not be updated
     * @throws JobConfigurationException
     *         if the job is incorrectly configured
     */
    public Job updateJob(@NotNull final Job job) {

        if (Strings.isNullOrEmpty(job.getId())) {
            throw new NotUpdateableException("Unable to update a job with no id");
        }

        Job currentJob = getJob(job.getId()).orElseThrow(NotFoundException::new);

        if (!JobStatus.UNSUBMITTED.equals(currentJob.getStatus()) && !JobStatus.FAILED.equals(currentJob.getStatus())) {
            throw new NotUpdateableException("Unable to update a job with status " + currentJob.getStatus() + " only UNSUBMITTED an FAILED jobs can be updated");
        }

        // status updates happen via their own api methods
        if (!currentJob.getStatus().equals(job.getStatus())) {
            throw new NotUpdateableException("The new status does not match the current status");
        }

        if (!currentJob.getJobType().equals(job.getJobType())) {
            throw new NotUpdateableException("Changing the job type is not supported");
        }

        validateJobPriority(job);

        // "-jobConfiguration ensures the job's configuration is not updated via this method
        return jobDAO.update(job, new NodeFilter("-jobConfiguration", NodeFilter.DEPTH_INFINITE));
    }

    /**
     * Deletes the job for the given job id. Queued and Executing Jobs can not be removed.
     *
     * @param jobId
     *         the job id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     * @throws NotFoundException
     *         if an job does not exist for the given job id.
     * @throws NotUpdateableException
     *         if the job can not be deleted
     */
    public void deleteJob(@NotNull final String jobId) {
        Job job = getJob(jobId).orElseThrow(NotFoundException::new);
        if (JobStatus.QUEUED.equals(job.getStatus()) || JobStatus.EXECUTING.equals(job.getStatus())) {
            throw new NotUpdateableException("Unable to delete job. Job " + jobId + " has a status of " + job.getStatus());
        }
        jobDAO.remove(getJobPath(Optional.of(jobId)));
    }

    /**
     * Deletes the job for the given job id. Queued and Executing Jobs can not be removed.
     *
     * @param jobId
     *         the job id.
     * @param force
     *         deletes the job in any state. May leave orphaned executing tasks.
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     * @throws NotFoundException
     *         if an job does not exist for the given job id.
     * @throws NotUpdateableException
     *         if the job can not be deleted
     */
    public void deleteJob(@NotNull final String jobId, boolean force) {
        if (!force) {
            deleteJob(jobId);
        } else {
            jobDAO.remove(getJobPath(Optional.of(jobId)));
        }
    }

    /**
     * Updates a job's configuration for the given job id.
     *
     * @param jobId
     *         the job id
     * @param jobConfiguration
     *         the job configuration
     * @return the updated job configuration
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     * @throws NotFoundException
     *         if a job does not exist for the given job id.
     * @throws InvalidPropertyException
     *         if the job configuration contains invalid properties
     * @throws NotUpdateableException
     *         if the job can not be updated
     * @throws JobConfigurationException
     *         if the job is incorrectly configured
     */
    public JobConfiguration updateJobConfiguration(@NotNull final String jobId, @NotNull JobConfiguration jobConfiguration) {
        jobConfiguration.validate(jobConfigurationValidator);
        Job currentJob = getJob(jobId).orElseThrow(NotFoundException::new);
        currentJob.setJobConfiguration(jobConfiguration);
        Job updatedJob = jobDAO.update(currentJob);
        return updatedJob.getJobConfiguration();
    }

    /**
     * Updates the job status for the specified job. Only supports transitioning to QUEUED or CANCELED.
     *
     * @param jobId
     *         the job id
     * @param status
     *         the new job status
     * @return the updated job
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     * @throws NotFoundException
     *         if a job does not exist for the given job id.
     * @throws NotUpdateableException
     *         if the job can not be updated
     * @throws JobConfigurationException
     *         if the job is incorrectly configured
     */
    public Job updateJobStatus(@NotNull final String jobId, String status) {
        JobStatus jobStatus;
        try {
            jobStatus = JobStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyException("Unsupported job status", e);
        }
        return updateJobStatus(jobId, jobStatus);
    }

    /**
     * Updates the job status for the specified job. Only supports transitioning to QUEUED or CANCELED.
     *
     * @param jobId
     *         the job id
     * @param status
     *         the new job status
     * @return the updated job
     * @throws NotAuthorizedException
     *         if the user does not have access to the job.
     * @throws NotFoundException
     *         if a job does not exist for the given job id.
     * @throws NotUpdateableException
     *         if the job can not be updated
     * @throws JobConfigurationException
     *         if the job is incorrectly configured
     */
    public Job updateJobStatus(@NotNull final String jobId, JobStatus status) {

        Job currentJob = getJob(jobId).orElseThrow(NotFoundException::new);

        // Facade only supports transitioning job to queued and cancelled. Internal transitions should be validated via JobStatusTransition and executed using JobDAO
        if (JobStatus.QUEUED.equals(status)) {
            return submitJob(currentJob);
        } else if (JobStatus.CANCELED.equals(status)) {
            //TODO implement task cancellation
            throw new NotUpdateableException("Job status can not be updated to " + status);
        } else {
            throw new NotUpdateableException("Job status can not be updated to " + status);
        }
    }

    /**
     * Adds a {@link Message} to the given job.
     *
     * @param jobId
     *         the job id
     * @param message
     *         the message
     */
    public void addMessageToJob(@NotNull final String jobId, @NotNull Message message) {
        Job job = getJob(jobId).orElseThrow(() -> new NotFoundException("Job " + jobId + " does not exist"));
        messageDAO.create(job.getNodePath(), message);
    }

    /**
     * Returns all of the job and task messages for the given job that match the given search constraints.
     *
     * @param jobId
     *         the job id
     * @param search
     *         the search constraints
     * @return all of the job and task messages for the given job that match the given search constraints.
     */
    public SearchResult<Message> findJobMessages(@NotNull String jobId, @NotNull Search search) {
        Job job = getJob(jobId).orElseThrow(() -> new NotFoundException("Job " + jobId + " does not exist"));
        return messageDAO.findJobMessages(job.getNodePath(), search);
    }

    /**
     * Handles pipeline callbacks and updates the associated job accordingly.
     *
     * @param callbackEvent
     *         the transfer task callback event
     */
    public void onPipelineCallback(@Observes @CallbackListener(JOB_CALLBACK_LISTENER) final CallbackEvent callbackEvent) {
        Optional<Job> jobForCallbackEvent = getJobForCallbackEvent(callbackEvent);

        jobForCallbackEvent.ifPresent(job -> getJobStatusForCallbackEvent(callbackEvent).ifPresent(status -> {
            JobStatus currentStatus = job.getStatus();
            if (!currentStatus.equals(status)) {
                jobStatusTransition.transition(job, status);
            }

            List<String> messages = callbackEvent.getProperties().get("message").asList();
            if (!messages.isEmpty()) {
                MessageType messageType = Optional.of(job.getStatus()).filter(jobStatus -> jobStatus.equals(JobStatus.FAILED)).map(taskStatus -> MessageType.ERROR).orElse(MessageType.INFO);
                messages.forEach(msg -> messageDAO.create(job.getNodePath(), new Message(msg, messageType)));
            }

            if (!currentStatus.equals(status) || !messages.isEmpty()) {
                jobDAO.update(job);
            }
        }));
    }

    private Optional<Job> getJobForCallbackEvent(CallbackEvent callbackEvent) {
        Optional<Job> job = getJob(callbackEvent.getObjectId());
        if (!job.isPresent()) {
            LOGGER.error("Job " + callbackEvent.getObjectId() + " does not exist");
        }
        return job;
    }

    private String getJobPath(Optional<String> jobId) {
        String path = jobsNodePath;
        if (jobId.isPresent()) {
            String id = jobId.get();
            path = jobDAO.getDistributedNodePath(path, id);
        }
        return path;
    }

    private Job submitJob(Job job) {
        LOGGER.debug("Submitting Job " + job.getId());

        job.getJobConfiguration().validate(jobConfigurationValidator);
        submitJobEvent.fire(new SubmitJob(job.getJobConfiguration().getVariant()));
        jobStatusTransition.transition(job, JobStatus.QUEUED);

        Job updatedJob = jobDAO.update(job);
        createAndExecutePipeline(updatedJob);
        return updatedJob;
    }

    private void createAndExecutePipeline(Job job) {
        pipelineManager.execute(pipelineBuilder.build(job, JOB_CALLBACK_LISTENER));
    }

    private static Optional<JobStatus> getJobStatusForCallbackEvent(CallbackEvent callbackEvent) {
        Optional<JobStatus> status = callbackEvent.getProperties().get("status").stream().findFirst().map(JobStatus::valueOf);
        if (!status.isPresent()) {
            LOGGER.error("Unable to process callback for job " + callbackEvent.getObjectId() + " no status was provided");
        }
        return status;
    }

    private static void validateJob(Job job) {
        if (job.getJobType() == null) {
            throw new InvalidPropertyException("Type can not be null");
        }

        if (!JobStatus.UNSUBMITTED.equals(job.getStatus()) && !JobStatus.QUEUED.equals(job.getStatus())) {
            throw new InvalidPropertyException("Invalid Job Status " + job.getStatus() + ", the value must be UNSUBMITTED or QUEUED");
        }

        validateJobPriority(job);

        if (job.getJobConfiguration() == null) {
            throw new InvalidPropertyException("Configuration can not be null");
        }
    }

    private static void validateJobPriority(Job job) {
        if (!Range.closed(1L, 10L).contains(job.getPriority())) {
            throw new InvalidPropertyException("Invalid priority " + job.getPriority() + ", the value must be between 1 and 10");
        }
    }
}
