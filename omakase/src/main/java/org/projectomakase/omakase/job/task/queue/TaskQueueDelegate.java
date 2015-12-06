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

import org.projectomakase.omakase.task.api.Task;

import java.util.Optional;

/**
 * Implementations of this interface expose queue provider specific implementations of the task queue.
 *
 * @author Richard Lucas
 */
public interface TaskQueueDelegate {

    /**
     * Returns the Task Queue Delegate type e.g. ACTIVEMQ, SQS, etc.
     *
     * @return the Task Queue Delegate type e.g. ACTIVEMQ, SQS, etc.
     */
    String getType();

    /**
     * Adds a new task id to the queue.
     *
     * @param task
     *         the task
     */
    void add(Task task);

    /**
     * Gets the next eligible task id from the queue for the given task type.
     *
     * @param type
     *         the task type
     * @return the next eligible task id from the queue.
     */
    Optional<String> get(String type);

    /**
     * Drains the queue of any remaining messages.
     *
     */
    void drain();
}
