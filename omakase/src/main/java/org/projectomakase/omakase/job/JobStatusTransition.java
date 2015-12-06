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
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.jboss.logging.Logger;

import java.util.Date;

/**
 * Encapsulates the logic for transitioning a Job' status.
 *
 * @author Richard Lucas
 */
public class JobStatusTransition {

    private static final Logger LOGGER = Logger.getLogger(JobStatusTransition.class);

    /**
     * Transitions the specified job from it's current status to it's new status.
     *
     * @param job
     *         the job being transitioned
     * @param newStatus
     *         the new job status
     * @throws NotUpdateableException
     *         thrown if the job's  status can not be updated.
     */
    public void transition(Job job, JobStatus newStatus) {
        if (isValidStatusChange(job.getStatus(), newStatus)) {
            job.setStatus(newStatus);
            job.setStatusTimestamp(new Date());
        } else {
            String message = "Job status can not be updated from " + job.getStatus() + " to " + newStatus;
            LOGGER.info(message);
            throw new NotUpdateableException(message);
        }
    }

    private static boolean isValidStatusChange(JobStatus currentStatus, JobStatus newStatus) {
        boolean result = false;
        switch (currentStatus) {
            case UNSUBMITTED:
            case FAILED:
                result = validateNewStatus(ImmutableList.of(JobStatus.QUEUED), newStatus);
                break;
            case QUEUED:
                result = validateNewStatus(ImmutableList.of(JobStatus.EXECUTING, JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELED), newStatus);
                break;
            case EXECUTING:
                result = validateNewStatus(ImmutableList.of(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELED), newStatus);
                break;
            case COMPLETED:
            case CANCELED:
            default:
                break;
        }
        return result;
    }

    private static boolean validateNewStatus(ImmutableList<JobStatus> validStatuses, JobStatus newStatus) {
        boolean result = false;
        if (validStatuses.contains(newStatus)) {
            result = true;
        }
        return result;
    }
}
