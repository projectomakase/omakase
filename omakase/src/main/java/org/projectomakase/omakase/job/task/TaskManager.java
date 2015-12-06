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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.callback.Callback;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.streams.Streams;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageDAO;
import org.projectomakase.omakase.job.message.MessageType;
import org.projectomakase.omakase.job.task.jcr.TaskGroupNode;
import org.projectomakase.omakase.job.task.jcr.TaskGroupNodeDAO;
import org.projectomakase.omakase.job.task.jcr.TaskNode;
import org.projectomakase.omakase.job.task.jcr.TaskNodeDAO;
import org.projectomakase.omakase.job.task.queue.TaskQueue;
import org.projectomakase.omakase.job.task.queue.TaskStatusQueue;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchException;
import org.projectomakase.omakase.search.SearchResult;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.projectomakase.omakase.commons.collectors.ImmutableSetCollector.toImmutableSet;

/**
 * Java facade for managing tasks
 *
 * @author Richard Lucas
 */
@Named
@Stateless
public class TaskManager {

    private static final Logger LOGGER = Logger.getLogger(TaskManager.class);

    @Inject
    @OrganizationNodePath()
    String organizationNodePath;
    @Inject
    Tasks tasks;
    @Inject
    TaskGroupNodeDAO taskGroupDAO;
    @Inject
    TaskNodeDAO taskDAO;
    @Inject
    MessageDAO messageDAO;
    @Inject
    IdGenerator idGenerator;
    @Inject
    TaskQueue taskQueue;
    @Inject
    TaskStatusQueue taskStatusQueue;
    @Inject
    Callback callback;

    /**
     * Creates a new task group.
     *
     * @param taskGroup
     *         the task group to create
     * @return the created task group.
     * @throws InvalidPropertyException
     *         if the task group contains an invalid property
     * @throws NotAuthorizedException
     *         if the user does not have permission to create task groups.
     */
    public TaskGroup createTaskGroup(@NotNull final TaskGroup taskGroup) {
        TaskGroupNode taskGroupNode = tasks.fromTaskGroup(taskGroup);
        String taskGroupId = idGenerator.getId();
        taskGroupNode.setId(taskGroupId);
        TaskGroupNode createdTaskGroupNode = taskGroupDAO.create(getRootPath(taskGroup.getJobId()), taskGroupNode);
        return tasks.fromTaskGroupNode(createdTaskGroupNode);
    }

    /**
     * Returns all of the task groups for a given job id.
     *
     * @param jobId
     *         the job id
     * @return all of the task groups for a given job id.
     */
    public ImmutableSet<TaskGroup> getTaskGroups(String jobId) {
        return taskGroupDAO.findTaskGroupsForJob(jobId).stream().map(tasks::fromTaskGroupNode).collect(toImmutableSet());
    }

    /**
     * Returns the task group for the given task group id if it exists, otherwise returns an empty optional.
     *
     * @param taskGroupId
     *         the task group id
     * @return the task group for the given task group id if it exists, otherwise returns an empty optional.
     */
    public Optional<TaskGroup> getTaskGroup(@NotNull final String taskGroupId) {
        return taskGroupDAO.findById(taskGroupId).map(tasks::fromTaskGroupNode);
    }

    /**
     * Creates a new task and adds it to the the given task group.
     * <p>
     * Validates the task has a status of QUEUED and a priority with a value between 1 and 10.
     * </p>
     *
     * @param taskGroupId
     *         the task group id
     * @param task
     *         the task to create.
     * @return the created task.
     * @throws InvalidPropertyException
     *         if the task contains an invalid property
     * @throws NotAuthorizedException
     *         if the user does not have permission to create tasks.
     */
    public Task createTask(@NotNull String taskGroupId, @NotNull final Task task) {
        TaskGroup taskGroup = getTaskGroup(taskGroupId).orElseThrow(() -> new NotFoundException("Task Group " + taskGroupId + " not found"));
        return createTask(taskGroup, task);
    }

    /**
     * Creates a new task within the specified task group, and queues it for processing by a worker.
     * <p>
     * Validates the task has a status of QUEUED and a priority with a value between 1 and 10.
     * </p>
     *
     * @param taskGroup
     *         the task group
     * @param task
     *         the task to create.
     * @return the created task.
     * @throws InvalidPropertyException
     *         if the task contains an invalid property
     * @throws NotAuthorizedException
     *         if the user does not have permission to create tasks.
     */
    public Task createTask(@NotNull TaskGroup taskGroup, @NotNull final Task task) {
        return createTask(taskGroup, task, true);
    }


    /**
     * Creates a new task within the specified task group.
     * <p>
     * Validates the task has a status of QUEUED and a priority with a value between 1 and 10.
     * </p>
     *
     * @param taskGroup
     *         the task group
     * @param task
     *         the task to create.
     * @param queueTask
     *         true if the task should be queued for processing by a worker, otherwise false e.g. tasks that are updated by external notifications.
     * @return the created task.
     * @throws InvalidPropertyException
     *         if the task contains an invalid property
     * @throws NotAuthorizedException
     *         if the user does not have permission to create tasks.
     */
    public Task createTask(@NotNull TaskGroup taskGroup, @NotNull final Task task, boolean queueTask) {
        if (!TaskStatus.QUEUED.equals(task.getStatus())) {
            throw new InvalidPropertyException("Invalid task status '" + task.getStatus() + "', new tasks must have a status of 'QUEUED'");
        }

        if (!Range.closed(1L, 10L).contains(task.getPriority())) {
            throw new InvalidPropertyException("Invalid task priority value '" + task.getPriority() + "', the value must be between 1 and 10");
        }

        String taskId = idGenerator.getId();

        TaskNode taskNode = tasks.fromTask(task);
        taskNode.setId(taskId);

        Task createdTask = tasks.fromTaskNode(taskDAO.create(getTaskGroupPath(taskGroup.getJobId(), taskGroup.getId()), taskNode));
        if (queueTask) {
            taskQueue.add(createdTask);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created " + createdTask.getType() + " task " + createdTask.getId() + " " + createdTask.getDescription());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(createdTask.getConfiguration());
        }

        return createdTask;
    }

    /**
     * Returns all of the tasks for the caller principles organization that match the given search constraints.
     *
     * @param search
     *         search constraints
     * @return all of the tasks for the caller principles organization that match the given search constraints.
     * @throws SearchException
     *         if the search fails
     */
    public SearchResult<Task> findJobs(@NotNull final Search search) {
        SearchResult<TaskNode> taskNodeSearchResults = taskDAO.findNodes(organizationNodePath + "/jobs", search, "omakase:task");
        return new SearchResult<>(taskNodeSearchResults.getRecords().stream().map(tasks::fromTaskNode).collect(Collectors.toList()), taskNodeSearchResults.getTotalRecords());
    }

    /**
     * Returns an immutable set of tasks for the given task group.
     *
     * @param taskGroupId
     *         the task group id
     * @return an immutable set of tasks for the given task group.
     */
    public ImmutableSet<Task> getTasks(@NotNull final String taskGroupId) {
        return taskDAO.findTasksForGroup(taskGroupId).stream().map(tasks::fromTaskNode).collect(ImmutableSetCollector.toImmutableSet());
    }

    /**
     * Returns the task for the given taskId if it exists otherwise returns an empty Optional.
     *
     * @param taskId
     *         the task id.
     * @return the task for the given taskId if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the task.
     */
    public Optional<Task> getTask(final String taskId) {
        return taskDAO.findById(taskId).map(tasks::fromTaskNode);
    }

    /**
     * Updates the task status and notifies any observers of the update by firing a TaskCallbackEvent.
     * <p>
     * This is a synchronous call and updates the task status immediately.
     * </p>
     *
     * @param taskId
     *         the task id
     * @param taskStatusUpdate
     *         the task status update
     * @return the updated task
     * @throws NotAuthorizedException
     *         if the user does not have access to the task.
     * @throws NotFoundException
     *         if the task does not exist.
     */
    public Task updateTaskStatus(@NotNull final String taskId, @NotNull final TaskStatusUpdate taskStatusUpdate) {

        TaskNode currentTaskNode = taskDAO.findById(taskId).orElseThrow(() -> new NotFoundException("Unable to find task " + taskId));

        if (TaskStatus.COMPLETED.equals(currentTaskNode.getStatus()) || tasks.isFailedTaskStatus(currentTaskNode.getStatus())) {
            LOGGER.warn("Task is in already terminating state and can not be updated");
            return tasks.fromTaskNode(currentTaskNode);
        } else {
            TaskNode updatedTaskNode = updateTaskNodeWithTaskStatusUpdate(taskStatusUpdate, currentTaskNode);

            TaskGroupNode taskGroupNode = taskGroupDAO.findByTaskId(taskId).orElseThrow(() -> new NotFoundException("Unable to find task group for task " + taskId));
            TaskStatus newGroupStatus = tasks.getTaskGroupStatusFromTasks(getTasks(taskGroupNode.getId()));
            TaskGroupNode updatedTaskGroup = updateTaskGroupStatus(taskGroupNode, newGroupStatus);

            ImmutableMultimap.Builder<String, String> mapBuilder = ImmutableMultimap.builder();
            mapBuilder.put("taskGroupId", updatedTaskGroup.getId());
            mapBuilder.put("taskGroupStatus", updatedTaskGroup.getStatus().name());
            mapBuilder.put("taskId", taskId);

            Optional.ofNullable(taskStatusUpdate.getMessage()).ifPresent(message -> {
                MessageType messageType = Optional.of(taskStatusUpdate.getStatus()).filter(tasks::isFailedTaskStatus).map(taskStatus -> MessageType.ERROR).orElse(MessageType.INFO);
                messageDAO.create(updatedTaskNode.getNodePath(), new Message(message, messageType));
            });

            CallbackEvent callbackEvent = new CallbackEvent(updatedTaskGroup.getPipelineId(), mapBuilder.build());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(callbackEvent);
            }
            callback.fire(callbackEvent, Optional.ofNullable(updatedTaskGroup.getCallbackListenerId()).orElse(""));
            Task task = tasks.fromTaskNode(updatedTaskNode);

            // re-queue the task as it has been reset to queued in order for it to be retried
            if (TaskStatus.QUEUED.equals(task.getStatus())) {
                taskQueue.add(task);
            }

            return task;
        }
    }

    /**
     * Adds a {@link Message} to the given task.
     *
     * @param taskId
     *         the task id
     * @param message
     *         the message
     */
    public void addMessageToTask(@NotNull final String taskId, @NotNull Message message) {
        TaskNode taskNode = taskDAO.findById(taskId).orElseThrow(() -> new NotFoundException("Unable to find task " + taskId));
        messageDAO.create(taskNode.getNodePath(), message);
    }


    /**
     * Retrieves tasks from the task queue.
     *
     * @param taskType
     *         the task type
     * @param max
     *         the maximum number of tasks to retrieve
     * @return an immutable list of tasks that match the given task limited by the specified max. value.
     */
    public ImmutableList<Task> retrieveTasksFromTaskQueue(String taskType, int max) {
        return taskQueue.get(taskType, max).stream().map(this::getTask).flatMap(Streams::optionalToStream).filter(task -> TaskStatus.QUEUED.equals(task.getStatus())).collect(
                ImmutableListCollector.toImmutableList());
    }

    /**
     * Adds a task status update to the task status queue for asynchronous processing.
     *
     * @param taskId
     *         the task id
     * @param taskStatusUpdate
     *         the task status update
     */
    public void addTaskStatusUpdateToQueue(@NotNull String taskId, @NotNull TaskStatusUpdate taskStatusUpdate) {
        taskStatusQueue.add(taskId, taskStatusUpdate);
    }

    private String getRootPath(String jobId) {
        return taskDAO.getDistributedNodePath(organizationNodePath + "/jobs", jobId);
    }

    private String getTaskGroupPath(String jobId, String taskGroupId) {
        return taskDAO.getDistributedNodePath(organizationNodePath + "/jobs", jobId + "/" + taskGroupId);
    }

    private TaskNode updateTaskNodeWithTaskStatusUpdate(@NotNull TaskStatusUpdate taskStatusUpdate, TaskNode currentTaskNode) {
        TaskNode updatedTask;
        if (tasks.shouldRetryTask(taskStatusUpdate.getStatus(), Ints.checkedCast(currentTaskNode.getRetryAttempts()), tasks.getMaxTaskRetries())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrying task " + currentTaskNode.getId());
            }
            updatedTask = taskDAO.update(new TaskNode(currentTaskNode, TaskStatus.QUEUED, null, currentTaskNode.getRetryAttempts() + 1L));
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updating task " + currentTaskNode.getId() + " status to " + taskStatusUpdate.getStatus());
            }
            updatedTask = taskDAO.update(new TaskNode(currentTaskNode, taskStatusUpdate.getStatus(), taskStatusUpdate.getOutput().orElse(null), currentTaskNode.getRetryAttempts()));
        }
        return updatedTask;
    }

    private TaskGroupNode updateTaskGroupStatus(TaskGroupNode taskGroupNode, TaskStatus taskStatus) {
        if (!TaskStatus.QUEUED.equals(taskStatus) && !taskGroupNode.getStatus().equals(taskStatus)) {
            taskGroupNode.setStatus(taskStatus);
            taskGroupNode.setStatusTimestamp(new Date());
            return taskGroupDAO.update(taskGroupNode);
        } else {
            return taskGroupNode;
        }
    }
}
