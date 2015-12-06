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

import org.projectomakase.omakase.task.spi.TaskOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a task status update
 *
 * @author Richard Lucas
 */
public class TaskStatusUpdate {

    private final TaskStatus status;
    private final String message;
    private final int percentageComplete;
    private final TaskOutput output;

    public TaskStatusUpdate(TaskStatus status, String message, int percentageComplete) {
        this.status = status;
        this.message = message;
        this.percentageComplete = percentageComplete;
        this.output = null;
    }

    public TaskStatusUpdate(TaskStatus status, String message, int percentageComplete, TaskOutput output) {
        this.status = status;
        this.message = message;
        this.percentageComplete = percentageComplete;
        this.output = output;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Integer getPercentageComplete() {
        return percentageComplete;
    }

    public Optional<TaskOutput> getOutput() {
        return Optional.ofNullable(output);
    }

    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format("\"status\":\"%s\"", status.name()));
        if (message != null && message.length() > 0) {
            attributes.add(String.format("\"message\":\"%s\"", message));
        }
        if (percentageComplete != -1) {
            attributes.add(String.format("\"percent_complete\":%d", percentageComplete));
        }
        getOutput().ifPresent(out -> attributes.add("\"output\":" + out.toJson()));
        return "{" + String.join(",", attributes) + "}";
    }

    @Override
    public String toString() {
        return "TaskStatusUpdate{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", percentageComplete=" + percentageComplete +
                ", output=" + output +
                '}';
    }
}
