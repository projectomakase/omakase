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

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.job.task.jcr.TaskGroupNode;
import org.projectomakase.omakase.job.task.jcr.TaskNode;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskInstanceLoader;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.task.spi.TaskOutput;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility methods for working with the task API.
 *
 * @author Richard Lucas
 */
@ApplicationScoped
public class Tasks {

    private static final Map<TaskStatus, Integer> taskStatusWeight =
            ImmutableMap.of(TaskStatus.QUEUED, 2, TaskStatus.EXECUTING, 1, TaskStatus.COMPLETED, 5, TaskStatus.FAILED_DIRTY, 3, TaskStatus.FAILED_CLEAN, 4);

    @Inject
    TaskManager taskManager;
    @Inject
    @ConfigProperty(name = "omakase.max.task.retries")
    int maxTaskRetries;

    /**
     * Maps a {@link TaskNode} to a {@link Task}.
     *
     * @param taskNode
     *         the {@link TaskNode}
     * @return the {@link Task}.
     */
    public Task fromTaskNode(TaskNode taskNode) {
        TaskConfiguration taskConfiguration = TaskInstanceLoader.loadTaskConfigurationInstance(taskNode.getType());
        taskConfiguration.fromJson(taskNode.getConfiguration());

        Optional<TaskOutput> taskOutput = Optional.ofNullable(taskNode.getOutput()).map(out -> TaskInstanceLoader.loadTaskOutputInstance(taskNode.getType()));
        taskOutput.ifPresent(out -> out.fromJson(taskNode.getOutput()));

        return new Task(taskNode.getId(), taskNode.getType(), taskNode.getDescription(), taskNode.getStatus(), fromDate(taskNode.getStatusTimestamp()), taskNode.getPriority(), taskConfiguration,
                taskOutput.orElse(null), fromDate(taskNode.getCreated()));
    }

    /**
     * Maps a {@link Task} to a {@link TaskNode}.
     *
     * @param task
     *         the {@link Task}
     * @return the {@link TaskNode}.
     */
    public TaskNode fromTask(Task task) {
        String output = task.getOutput().map(TaskOutput::toJson).orElse(null);
        return new TaskNode(task.getId(), task.getType(), task.getDescription(), task.getStatus(), Date.from(task.getStatusTimestamp().toInstant()), task.getPriority(),
                task.getConfiguration().toJson(), output);
    }

    /**
     * Maps a {@link TaskGroupNode} to a {@link TaskGroup}.
     *
     * @param taskGroupNode
     *         the {@link TaskGroupNode}
     * @return the {@link TaskGroup}.
     */
    public TaskGroup fromTaskGroupNode(TaskGroupNode taskGroupNode) {
        return new TaskGroup(taskGroupNode.getId(), taskGroupNode.getStatus(), fromDate(taskGroupNode.getStatusTimestamp()), taskGroupNode.getJobId(), taskGroupNode.getPipelineId(),
                taskGroupNode.getCallbackListenerId(), fromDate(taskGroupNode.getCreated()));
    }

    /**
     * Maps a {@link TaskGroup} to a {@link TaskGroupNode}.
     *
     * @param taskGroup
     *         the {@link TaskGroup}
     * @return the {@link TaskGroupNode}.
     */
    public TaskGroupNode fromTaskGroup(TaskGroup taskGroup) {
        return new TaskGroupNode(taskGroup.getId(), taskGroup.getStatus(), Date.from(taskGroup.getStatusTimestamp().toInstant()), taskGroup.getJobId(), taskGroup.getPipelineId(),
                taskGroup.getCallbackListenerId());
    }

    /**
     * Creates a {@link TaskStatusUpdate} from a json string.
     *
     * @param taskId
     *         the task id the update is related to.
     * @param json
     *         a json string representation of the update
     * @return a {@link TaskStatusUpdate}.
     */
    public TaskStatusUpdate taskStatusUpdateFromJson(String taskId, String json) {
        Task task = taskManager.getTask(taskId).orElseThrow(() -> new NotFoundException("task " + taskId + " does not exist"));
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            return new TaskStatusUpdate(getTaskStatus(jsonObject), getMessage(jsonObject), getPercentComplete(jsonObject), getTaskOutput(jsonObject, TaskInstanceLoader.loadTaskOutputInstance(task.getType())));
        }
    }

    /**
     * Returns the task group status based on a list of task statuses.
     *
     * @param tasks
     *         a list of tasks
     * @return the task group status.
     */
    public TaskStatus getTaskGroupStatusFromTasks(Set<Task> tasks) {
        return tasks.stream().map(Task::getStatus).sorted(Comparator.comparing(taskStatusWeight::get)).findFirst().get();
    }

    /**
     * Returns the task group status based on a list of task statuses.
     *
     * @param taskStatuses
     *         a list of task statuses
     * @return the task group status
     */
    public TaskStatus getTaskGroupStatusFromTaskStatuses(Set<TaskStatus> taskStatuses) {
        return taskStatuses.stream().sorted(Comparator.comparing(taskStatusWeight::get)).findFirst().get();
    }

    /**
     * Returns true if the task status is a failure statue otherwise true.
     *
     * @param taskStatus
     *         the task status
     * @return true if the task status is a failure statue otherwise true.
     */
    public boolean isFailedTaskStatus(TaskStatus taskStatus) {
        return TaskStatus.FAILED_CLEAN.equals(taskStatus) || TaskStatus.FAILED_DIRTY.equals(taskStatus);
    }

    /**
     * Returns the maximum number of times a task should be retired before marking as failed.
     *
     * @return the maximum number of times a task should be retired before marking as failed.
     */
    public int getMaxTaskRetries() {
        if (maxTaskRetries > 10) {
            return 10;
        } else if (maxTaskRetries < 0) {
            return 0;
        } else {
            return maxTaskRetries;
        }
    }

    /**
     * Returns true if the task should be retried otherwise false.
     *
     * @param taskStatus
     *         the task status
     * @param currentRetryAttempts
     *         the current number of retry attempts
     * @param maxRetryAttempts
     *         the maximum number of retry attempts
     * @return true if the task should be retried otherwise false.
     */
    public boolean shouldRetryTask(TaskStatus taskStatus, int currentRetryAttempts, int maxRetryAttempts) {
        return isFailedTaskStatus(taskStatus) && maxRetryAttempts > currentRetryAttempts;
    }

    private static ZonedDateTime fromDate(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static TaskStatus getTaskStatus(JsonObject jsonObject) {
        if (!jsonObject.containsKey("status")) {
            throw new IllegalArgumentException("Invalid JSON, requires a 'status' property");
        } else {
            return TaskStatus.valueOf(jsonObject.getString("status"));
        }
    }

    private static String getMessage(JsonObject jsonObject) {
        if (jsonObject.containsKey("message")) {
            return jsonObject.getString("message");
        } else {
            return null;
        }
    }

    private static int getPercentComplete(JsonObject jsonObject) {
        if (jsonObject.containsKey("percent_complete")) {
            return jsonObject.getInt("percent_complete");
        } else {
            return -1;
        }
    }

    private static TaskOutput getTaskOutput(JsonObject jsonObject, TaskOutput outputInstance) {
        if (jsonObject.containsKey("output")) {
            outputInstance.fromJson(jsonObject.getJsonObject("output").toString());
            return outputInstance;
        } else {
            return null;
        }
    }
}
