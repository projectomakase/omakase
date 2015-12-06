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
package org.projectomakase.omakase.broker;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.event.UnregisterWorker;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageDAO;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.search.SearchException;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * Broker Java Facade.
 *
 * @author Richard Lucas
 */
@Stateless
public class BrokerManager {

    private static final Logger LOGGER = Logger.getLogger(BrokerManager.class);

    @Inject
    @OrganizationNodePath("workers")
    String workersNodePath;
    @Inject
    IdGenerator idGenerator;
    @Inject
    WorkerDAO workerDAO;
    @Inject
    MessageDAO messageDAO;
    @Inject
    TaskManager taskManager;
    @Inject
    Event<UnregisterWorker> unregisterWorkerEvent;

    /**
     * Registers a new worker with Omakase.
     *
     * @param worker
     *         the worker to register
     * @return the newly registered worker.
     */
    public Worker registerWorker(@NotNull final Worker worker) {
        String id = idGenerator.getId();
        worker.setId(id);
        return workerDAO.distributedCreate(getWorkerPath(Optional.empty()), worker, id);
    }

    /**
     * Returns all of the workers that match the given search.
     *
     * @param search
     *         the search
     * @return all of the workers that match the given search.
     * @throws SearchException
     *         if the search fails
     */
    public SearchResult<Worker> findWorkers(@NotNull final Search search) {
        return workerDAO.findNodes(getWorkerPath(Optional.empty()), search, "omakase:worker");
    }


    /**
     * Returns the worker for the given worker id if it exists otherwise returns an empty Optional.
     *
     * @param workerId
     *         the worker id.
     * @return the worker for the given worker id if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the worker.
     */
    public Optional<Worker> getWorker(@NotNull final String workerId) {
        return Optional.ofNullable(workerDAO.get(getWorkerPath(Optional.of(workerId))));
    }

    /**
     * Updates the given worker.
     *
     * @param worker
     *         the worker
     * @return the updated worker.
     */
    public Worker updateWorker(final Worker worker) {
        return workerDAO.update(worker);
    }

    /**
     * Unregister the worker for the given worker id.
     *
     * @param workerId
     *         the worker id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the worker.
     * @throws NotFoundException
     *         if an worker does not exist for the given worker id.
     * @throws NotUpdateableException
     *         if the worker can not be unregistered
     */
    public void unregisterWorker(@NotNull final String workerId) {
        unregisterWorkerEvent.fire(new UnregisterWorker(workerId));
        workerDAO.remove(getWorkerPath(Optional.of(workerId)));
    }

    /**
     * Returns a list of available tasks for the specified worker and capacity.
     *
     * @param workerId
     *         the id of the worker requesting the tasks
     * @param capacity
     *         the workers capacity
     * @return a list of available tasks for the specified worker and capacity.
     */
    public ImmutableList<Task> getNextAvailableTasksForWorker(String workerId, List<Capacity> capacity) {
        Worker worker = getWorker(workerId).orElseThrow(() -> new NotFoundException("Worker " + workerId + " does not exist"));
        return Optional.of(worker).filter(w -> w.getStatus().equals(WorkerStatus.ACTIVE)).map(w -> assignTasksToWorker(capacity, w)).orElse(ImmutableList.of());
    }

    /**
     * Handles a task status update from a worker.
     *
     * @param workerId
     *         the worker id.
     * @param taskId
     *         the task id.
     * @param taskStatusUpdate
     *         the task status update.
     */
    public void handleTaskStatusUpdateFromWorker(String workerId, String taskId, TaskStatusUpdate taskStatusUpdate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Added task status update for task " + taskId + " from worker " + workerId);
        }
        taskManager.addTaskStatusUpdateToQueue(taskId, taskStatusUpdate);
    }

    /**
     * Returns all of the task messages for the given worker that match the given search constraints.
     *
     * @param workerId
     *         the worker id
     * @param search
     *         the search constraints
     * @return all of the task messages for the given worker that match the given search constraints.
     */
    public SearchResult<Message> findWorkerMessages(@NotNull String workerId, @NotNull Search search) {
        return messageDAO.findWorkerMessages(workerId, search);
    }

    private ImmutableList<Task> assignTasksToWorker(List<Capacity> capacity, Worker worker) {
        return getTasks(capacity).stream().filter(task -> TaskStatus.QUEUED.equals(task.getStatus())).map(task -> assignTaskToWorker(task, worker)).collect(ImmutableListCollector.toImmutableList());
    }

    private ImmutableList<Task> getTasks(List<Capacity> capacity) {
        return capacity.stream().map(cap -> taskManager.retrieveTasksFromTaskQueue(cap.getType(), cap.getAvailability())).collect(ImmutableListsCollector.toImmutableList());
    }

    private Task assignTaskToWorker(Task task, Worker worker) {
        worker.getTasks().add(task.getId());
        workerDAO.update(worker);
        return taskManager.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.EXECUTING, "Assigned task to worker " + worker.getId(), 0));
    }

    private String getWorkerPath(Optional<String> workerId) {
        String path = workersNodePath;
        if (workerId.isPresent()) {
            String id = workerId.get();
            path = workerDAO.getDistributedNodePath(path, id);
        }
        return path;
    }
}
