/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker.tool;

import org.projectomakase.omakase.task.api.TaskStatusUpdate;

/**
 * Callback sent by the different tool implementations.
 *
 * @author Richard Lucas
 */
public class ToolCallback {

    private final String toolName;
    private final String taskId;
    private final TaskStatusUpdate taskStatusUpdate;

    public ToolCallback(String toolName, String taskId, TaskStatusUpdate taskStatusUpdate) {
        this.toolName = toolName;
        this.taskId = taskId;
        this.taskStatusUpdate = taskStatusUpdate;
    }

    public String getToolName() {
        return toolName;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatusUpdate getTaskStatusUpdate() {
        return taskStatusUpdate;
    }

    @Override
    public String toString() {
        return "ToolCallback{" +
                "toolName='" + toolName + '\'' +
                ", taskId='" + taskId + '\'' +
                ", taskStatusUpdate=" + taskStatusUpdate +
                '}';
    }
}
