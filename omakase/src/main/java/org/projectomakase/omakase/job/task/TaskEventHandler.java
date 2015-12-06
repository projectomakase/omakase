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

import org.projectomakase.omakase.event.UnregisterWorker;
import org.projectomakase.omakase.job.task.jcr.TaskNodeDAO;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * Implements observer methods for the different types of application events that need to be processed by the task framework.
 *
 * @author Richard Lucas
 */
public class TaskEventHandler {

    @Inject
    TaskManager taskManager;
    @Inject
    Tasks tasks;
    @Inject
    TaskNodeDAO taskDAO;

    public void handleUnregisterWorker(@Observes UnregisterWorker unregisterWorker) {
        taskDAO.findAllExecutingTasksAssociatedToWorker(unregisterWorker.getWorkerId()).stream().map(tasks::fromTaskNode).forEach(
                task -> taskManager.updateTaskStatus(task.getId(), createTaskStatusUpdate(unregisterWorker)));
    }

    private static TaskStatusUpdate createTaskStatusUpdate(UnregisterWorker unregisterWorker) {
        return new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Worker " + unregisterWorker.getWorkerId() + "was unregistered while executing this task.", -1);
    }

}
