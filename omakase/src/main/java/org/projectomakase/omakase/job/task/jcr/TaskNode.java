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
import org.projectomakase.omakase.task.spi.TaskOutput;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.Date;
import java.util.Optional;

/**
 * Task Node implementation that uses {@link org.jcrom.Jcrom} to serialize/deserialize to/from a JCR node.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:task"}, classNameProperty = "className")
public class TaskNode extends JcrEntity {

    public static final String TYPE = "omakase:type";
    public static final String DESCRIPTION = "omakase:description";
    public static final String STATUS = "omakase:status";
    public static final String STATUS_TIMESTAMP = "omakase:statusTimestamp";

    @JcrProperty(name = TYPE)
    private String type;
    @JcrProperty(name = DESCRIPTION)
    private String description;
    @JcrProperty(name = STATUS)
    private TaskStatus status = TaskStatus.QUEUED;
    @JcrProperty(name = STATUS_TIMESTAMP)
    private Date statusTimestamp = new Date();
    @JcrProperty(name = "omakase:priority")
    private long priority;
    @JcrProperty(name = "omakase:retryAttempts")
    private long retryAttempts;
    @JcrProperty(name = "omakase:configuration")
    private String configuration;
    @JcrProperty(name = "omakase:output")
    private String output;

    public TaskNode() {
        // required by JCROM
    }

    public TaskNode(String id, String type, String description, TaskStatus status, Date statusTimestamp, long priority, String configuration, String output) {
        this.name = id;
        this.type = type;
        this.description = description;
        this.status = status;
        this.statusTimestamp = statusTimestamp;
        this.priority = priority;
        this.retryAttempts = 0;
        this.configuration = configuration;
        this.output = output;
    }

    public TaskNode(TaskNode taskNode, TaskStatus taskStatus, TaskOutput taskOutput, long retryAttempts) {
        this.name = taskNode.getId();
        this.type = taskNode.getType();
        this.description = taskNode.getDescription();
        this.status = taskStatus;
        this.statusTimestamp = taskNode.getStatusTimestamp();
        this.priority = taskNode.getPriority();
        this.retryAttempts = retryAttempts;
        this.configuration = taskNode.getConfiguration();
        this.output = Optional.ofNullable(taskOutput).map(TaskOutput::toJson).orElse(null);
        this.path = taskNode.getNodePath();
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
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

    public long getPriority() {
        return priority;
    }

    public String getConfiguration() {
        return configuration;
    }

    public long getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(long retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "TaskNode{" +
                "type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", statusTimestamp=" + statusTimestamp +
                ", priority=" + priority +
                ", retryAttempts=" + retryAttempts +
                ", configuration='" + configuration + '\'' +
                ", output='" + output + '\'' +
                '}';
    }
}
