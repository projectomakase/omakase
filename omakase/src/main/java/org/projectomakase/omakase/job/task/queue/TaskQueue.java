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
package org.projectomakase.omakase.job.task.queue;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.task.api.Task;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Java Facade for interacting with the Omakase Task Queue.
 * <p>
 * The Omakase Task Queue is a scalable queue shared across all of the Omakase instances.
 * </p>
 *
 * @author Richard Lucas
 */
public class TaskQueue {

    private static final Logger LOGGER = Logger.getLogger(TaskQueue.class);

    @Inject
    @Omakase
    TaskQueueDelegate taskQueueDelegate;

    /**
     * Adds the specified task id onto the task queue.
     *
     * @param task
     *         the task
     */
    public void add(Task task) {
        taskQueueDelegate.add(task);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Added task " + task.getId() + " to task queue");
        }
    }

    /**
     * Retrieves the next n eligible tasks, up to the specified max value, that match the specified type from the queue.
     * <p>
     * This permanently removes the tasks from the queue. The exception to this is if the queue is transactional and the transaction is rolled back.
     * </p>
     *
     * @param type
     *         the task type
     * @param max
     *         the max number of tasks to retrieve
     * @return the next n eligible tasks, up to the specified max value, that match the specified type from the queue.
     */
    public ImmutableList<String> get(String type, int max) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();

        for (int i = 0; i < max; i++) {
            Optional<String> taskId = get(type);
            if (taskId.isPresent()) {
                builder.add(taskId.get());
            } else {
                break;
            }
        }

        return builder.build();
    }

    /**
     * Retrieves the next eligible task that matches the specified type from the queue.
     * <p>
     * This permanently removes the task from the queue. The exception to this is if the queue is transactional and the transaction is rolled back.
     * </p>
     *
     * @param type
     *         the task type
     * @return the next eligible task that matches the specified type from the queue or an empty Optional if no eligible tasks are available.
     */
    public Optional<String> get(String type) {
        Optional<String> taskId = taskQueueDelegate.get(type);
        taskId.ifPresent(id -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrieved task " + id + " from task queue");
            }
        });
        return taskId;
    }

    /**
     * Drains the queue of any remaining messages.
     *
     */
    public void drain() {
        taskQueueDelegate.drain();
    }
}
