/*
 * #%L
 * omakase-task
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
package org.projectomakase.omakase.task.api;

import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.task.spi.TaskOutput;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A task represents a long running activity that needs to be completed as part of a larger job.
 *
 * @author Richard Lucas
 */
public class Task {

    private String id;
    private String type;
    private String description;
    private TaskStatus status = TaskStatus.QUEUED;
    private ZonedDateTime statusTimestamp;
    private long priority;
    private TaskConfiguration configuration;
    private TaskOutput output;
    private ZonedDateTime created;

    /**
     * Creates a new task.
     *
     * @param type
     *         the task type
     * @param description
     *         a human readable description
     * @param priority
     *         the task's priority
     * @param configuration
     *         the tasks configuration
     */
    public Task(String type, String description, long priority, TaskConfiguration configuration) {
        this.type = type;
        this.description = description;
        this.priority = priority;
        this.configuration = configuration;

        ZonedDateTime now = ZonedDateTime.now();
        this.statusTimestamp = now;
        this.created = now;
    }

    // used internally to create a task instance
    public Task(String id, String type, String description, TaskStatus status, ZonedDateTime statusTimestamp, long priority, TaskConfiguration configuration, TaskOutput output,
                ZonedDateTime created) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.status = status;
        this.statusTimestamp = statusTimestamp;
        this.priority = priority;
        this.configuration = configuration;
        this.output = output;
        this.created = created;
    }

    // used internally to create a task instance
    public Task(String id, String type, String description, TaskConfiguration configuration) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.configuration = configuration;
    }

    public String getId() {
        return id;
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

    public ZonedDateTime getStatusTimestamp() {
        return statusTimestamp;
    }

    public long getPriority() {
        return priority;
    }

    public TaskConfiguration getConfiguration() {
        return configuration;
    }

    public Optional<TaskOutput> getOutput() {
        return Optional.ofNullable(output);
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
