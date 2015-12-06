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
package org.projectomakase.omakase.sns;

import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.job.task.queue.TaskStatusQueue;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.restore.RestoreTaskOutput;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.api.provider.DependentProvider;

import javax.json.JsonObject;
import java.net.URI;
import java.util.function.Consumer;

/**
 * Consumes Glacier ArchiveRetrieval notifications
 *
 * @author Richard Lucas
 */
public class ArchiveRetrievalNotificationConsumer implements Consumer<JsonObject> {

    DependentProvider<TaskStatusQueue> taskStatusQueueProvider;

    public ArchiveRetrievalNotificationConsumer() {
        taskStatusQueueProvider = BeanProvider.getDependent(TaskStatusQueue.class);
    }

    ArchiveRetrievalNotificationConsumer(DependentProvider<TaskStatusQueue> taskStatusQueueProvider) {
        this.taskStatusQueueProvider = taskStatusQueueProvider;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        String status = jsonObject.getString("StatusCode");
        String statusMessage = jsonObject.getString("StatusMessage");
        String taskId = jsonObject.getString("JobDescription");

        try {
            if ("Succeeded".equals(status)) {
                String jobId = jsonObject.getString("JobId");
                taskStatusQueueProvider.get().add(taskId, new TaskStatusUpdate(TaskStatus.COMPLETED, statusMessage, 100, new RestoreTaskOutput(Throwables.returnableInstance(() -> new URI(jobId)))));
            } else {
                taskStatusQueueProvider.get().add(taskId, new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, statusMessage, 0));
            }
        } finally {
            taskStatusQueueProvider.destroy();
        }
    }
}
