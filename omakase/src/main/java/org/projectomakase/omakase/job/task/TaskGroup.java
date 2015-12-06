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
package org.projectomakase.omakase.job.task;

import org.projectomakase.omakase.task.api.TaskStatus;

import java.time.ZonedDateTime;

/**
 * Task Group's are used to group related tasks together and provide a overall status for the group of tasks.
 * <p>
 * Task groups, and their tasks are created and observed by a job's pipeline which is responsible for determining what tasks are required to complete a job.
 * </p>
 *
 * @author Richard Lucas
 */
public class TaskGroup {

    private String id;
    private TaskStatus status = TaskStatus.QUEUED;
    private ZonedDateTime statusTimestamp;
    private String jobId;
    private String pipelineId;
    private String callbackListenerId;
    private ZonedDateTime created;

    /**
     * Creates a new task group.
     *
     * @param jobId
     *         the job id of the task groups job.
     * @param pipelineId
     *         the pipeline id of the pipeline that created the task group.
     * @param callbackListenerId
     *         the callback listener id of the observer listening the task group callback events.
     */
    public TaskGroup(String jobId, String pipelineId, String callbackListenerId) {
        this.jobId = jobId;
        this.pipelineId = pipelineId;
        this.callbackListenerId = callbackListenerId;

        ZonedDateTime now = ZonedDateTime.now();
        this.statusTimestamp = now;
        this.created = now;
    }

    // used internally to create a task instance
    TaskGroup(String id, TaskStatus status, ZonedDateTime statusTimestamp, String jobId, String pipelineId, String callbackListenerId, ZonedDateTime created) {
        this.id = id;
        this.status = status;
        this.statusTimestamp = statusTimestamp;
        this.jobId = jobId;
        this.pipelineId = pipelineId;
        this.callbackListenerId = callbackListenerId;
        this.created = created;
    }

    public String getId() {
        return id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public ZonedDateTime getStatusTimestamp() {
        return statusTimestamp;
    }

    public String getJobId() {
        return jobId;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getCallbackListenerId() {
        return callbackListenerId;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return "TaskGroup{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", statusTimestamp=" + statusTimestamp +
                ", jobId='" + jobId + '\'' +
                ", pipelineId='" + pipelineId + '\'' +
                ", callbackListenerId='" + callbackListenerId + '\'' +
                ", created=" + created +
                '}';
    }
}
