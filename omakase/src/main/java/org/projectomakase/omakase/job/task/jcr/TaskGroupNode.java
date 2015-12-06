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
package org.projectomakase.omakase.job.task.jcr;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.Date;

/**
 * Task Group Node implementation that uses {@link org.jcrom.Jcrom} to serialize/deserialize to/from a JCR node.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:taskGroup"})
public class TaskGroupNode extends JcrEntity {

    @JcrProperty(name = "omakase:status")
    private TaskStatus status = TaskStatus.QUEUED;
    @JcrProperty(name = "omakase:statusTimestamp")
    private Date statusTimestamp = new Date();
    @JcrProperty(name = "omakase:jobId")
    private String jobId;
    @JcrProperty(name = "omakase:pipelineId")
    private String pipelineId;
    @JcrProperty(name = "omakase:callbackListenerId")
    private String callbackListenerId;

    public TaskGroupNode() {
        // required by jcrom
    }

    public TaskGroupNode(String id, TaskStatus status, Date statusTimestamp, String jobId, String pipelineId, String callbackListenerId) {
        this.name = id;
        this.status = status;
        this.statusTimestamp = statusTimestamp;
        this.jobId = jobId;
        this.pipelineId = pipelineId;
        this.callbackListenerId = callbackListenerId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Date getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Date statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
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

    @Override
    public String toString() {
        return "TaskGroupNode{" +
                "id=" + name +
                ", status=" + status +
                ", statusTimestamp=" + statusTimestamp +
                ", jobId='" + jobId + '\'' +
                ", pipelineId='" + pipelineId + '\'' +
                ", callbackListenerId='" + callbackListenerId + '\'' +
                ", created=" + created +
                ", createdBy='" + createdBy + '\'' +
                ", lastModified=" + lastModified +
                ", lastModifiedBy='" + lastModifiedBy + '\'' +
                '}';
    }
}
