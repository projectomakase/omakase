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

import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class JobStatusTransitionTest {

    private JobStatusTransition transitioner;

    @Before
    public void before() {
        transitioner = new JobStatusTransition();
    }

    @Test
    public void shouldTransitionFromUnsubmittedToQueued() {
        transitionAndAssert(JobStatus.UNSUBMITTED, JobStatus.QUEUED);
    }

    @Test
    public void shouldTransitionFromQueuedToExecuting() {
        transitionAndAssert(JobStatus.QUEUED, JobStatus.EXECUTING);
    }

    @Test
    public void shouldTransitionFromQueuedToCompleted() {
        transitionAndAssert(JobStatus.QUEUED, JobStatus.COMPLETED);
    }

    @Test
    public void shouldTransitionFromQueuedToFailed() {
        transitionAndAssert(JobStatus.QUEUED, JobStatus.FAILED);
    }

    @Test
    public void shouldTransitionFromQueuedToCanceled() {
        transitionAndAssert(JobStatus.QUEUED, JobStatus.CANCELED);
    }

    @Test
    public void shouldTransitionFromExecutingToCanceled() {
        transitionAndAssert(JobStatus.EXECUTING, JobStatus.CANCELED);
    }

    @Test
    public void shouldTransitionFromExecutingToFailed() {
        transitionAndAssert(JobStatus.EXECUTING, JobStatus.FAILED);
    }

    @Test
    public void shouldTransitionFromExecutingToCompleted() {
        transitionAndAssert(JobStatus.EXECUTING, JobStatus.COMPLETED);
    }

    @Test
    public void shouldTransitionFromFailedToQueued() {
        transitionAndAssert(JobStatus.FAILED, JobStatus.QUEUED);
    }

    @Test
    public void shouldFailToTransitionFromUnsubmittedToUnsubmitted() {
        transitionAndAssertFailure(JobStatus.UNSUBMITTED, JobStatus.UNSUBMITTED);
    }

    @Test
    public void shouldFailToTransitionFromUnsubmittedToExecuting() {
        transitionAndAssertFailure(JobStatus.UNSUBMITTED, JobStatus.EXECUTING);
    }

    @Test
    public void shouldFailToTransitionFromUnsubmittedToFailed() {
        transitionAndAssertFailure(JobStatus.UNSUBMITTED, JobStatus.FAILED);
    }

    @Test
    public void shouldFailToTransitionFromUnsubmittedToCompleted() {
        transitionAndAssertFailure(JobStatus.UNSUBMITTED, JobStatus.COMPLETED);
    }

    @Test
    public void shouldFailToTransitionFromUnsubmittedToCanceled() {
        transitionAndAssertFailure(JobStatus.UNSUBMITTED, JobStatus.CANCELED);
    }

    @Test
    public void shouldFailToTransitionFromQueuedToUnsubmitted() {
        transitionAndAssertFailure(JobStatus.QUEUED, JobStatus.UNSUBMITTED);
    }

    @Test
    public void shouldFailToTransitionFromQueuedToQueued() {
        transitionAndAssertFailure(JobStatus.QUEUED, JobStatus.QUEUED);
    }

    @Test
    public void shouldFailToTransitionFromExecutingToUnsubmitted() {
        transitionAndAssertFailure(JobStatus.EXECUTING, JobStatus.UNSUBMITTED);
    }

    @Test
    public void shouldFailToTransitionFromExecutingToQueued() {
        transitionAndAssertFailure(JobStatus.EXECUTING, JobStatus.QUEUED);
    }

    @Test
    public void shouldFailToTransitionFromExecutingToExecuting() {
        transitionAndAssertFailure(JobStatus.EXECUTING, JobStatus.EXECUTING);
    }

    @Test
    public void shouldFailToTransitionFromCompletedToUnsubmitted() {
        transitionAndAssertFailure(JobStatus.COMPLETED, JobStatus.UNSUBMITTED);
    }

    @Test
    public void shouldFailToTransitionFromCompletedToQueued() {
        transitionAndAssertFailure(JobStatus.COMPLETED, JobStatus.QUEUED);
    }

    @Test
    public void shouldFailToTransitionFromCompletedToExecuting() {
        transitionAndAssertFailure(JobStatus.COMPLETED, JobStatus.EXECUTING);
    }

    @Test
    public void shouldFailToTransitionFromCompletedToFailed() {
        transitionAndAssertFailure(JobStatus.COMPLETED, JobStatus.FAILED);
    }

    @Test
    public void shouldFailToTransitionFromCompletedToCompleted() {
        transitionAndAssertFailure(JobStatus.COMPLETED, JobStatus.COMPLETED);
    }

    @Test
    public void shouldFailToTransitionFromCompletedToCanceled() {
        transitionAndAssertFailure(JobStatus.COMPLETED, JobStatus.CANCELED);
    }

    @Test
    public void shouldFailToTransitionFromFailedToUnsubmitted() {
        transitionAndAssertFailure(JobStatus.FAILED, JobStatus.UNSUBMITTED);
    }

    @Test
    public void shouldFailToTransitionFromFailedToExecuting() {
        transitionAndAssertFailure(JobStatus.FAILED, JobStatus.EXECUTING);
    }

    @Test
    public void shouldFailToTransitionFromFailedToFailed() {
        transitionAndAssertFailure(JobStatus.FAILED, JobStatus.FAILED);
    }

    @Test
    public void shouldFailToTransitionFromFailedToCompleted() {
        transitionAndAssertFailure(JobStatus.FAILED, JobStatus.COMPLETED);
    }

    @Test
    public void shouldFailToTransitionFromFailedToCanceled() {
        transitionAndAssertFailure(JobStatus.FAILED, JobStatus.CANCELED);
    }

    @Test
    public void shouldFailToTransitionFromCanceledToUnsubmitted() {
        transitionAndAssertFailure(JobStatus.CANCELED, JobStatus.UNSUBMITTED);
    }

    @Test
    public void shouldFailToTransitionFromCanceledToQueued() {
        transitionAndAssertFailure(JobStatus.CANCELED, JobStatus.QUEUED);
    }

    @Test
    public void shouldFailToTransitionFromCanceledToExecuting() {
        transitionAndAssertFailure(JobStatus.CANCELED, JobStatus.EXECUTING);
    }

    @Test
    public void shouldFailToTransitionFromCanceledToFailed() {
        transitionAndAssertFailure(JobStatus.CANCELED, JobStatus.FAILED);
    }

    @Test
    public void shouldFailToTransitionFromCanceledToCompleted() {
        transitionAndAssertFailure(JobStatus.CANCELED, JobStatus.COMPLETED);
    }

    @Test
    public void shouldFailToTransitionFromCanceledToCanceled() {
        transitionAndAssertFailure(JobStatus.CANCELED, JobStatus.CANCELED);
    }


    private void transitionAndAssert(JobStatus currentStatus, JobStatus newStatus) {
        Job job = Job.Builder.build(j -> j.setStatus(currentStatus));
        transitioner.transition(job, newStatus);
        assertThat(job.getStatus()).isEqualTo(newStatus);
    }

    private void transitionAndAssertFailure(JobStatus currentStatus, JobStatus newStatus) {
        Job job = Job.Builder.build(j -> j.setStatus(currentStatus));
        try {
            transitioner.transition(job, newStatus);
            failBecauseExceptionWasNotThrown(NotUpdateableException.class);
        } catch (NotUpdateableException e) {
            assertThat(e).hasMessage("Job status can not be updated from " + currentStatus + " to " + newStatus);
        }
    }


}