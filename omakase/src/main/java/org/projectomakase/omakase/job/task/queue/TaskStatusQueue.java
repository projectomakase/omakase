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

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.OmakaseCluster;
import org.projectomakase.omakase.camel.CamelQueueEndpoint;
import org.projectomakase.omakase.commons.compress.Compressors;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.job.task.Tasks;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

/**
 * Java Facade for interacting with the Omakase Task Status Queue.
 * <p>
 * The Omakase Task Status Queue is a scalable queue shared across all of the Omakase instances.
 * </p>
 * <p>
 * The Task Status Queue is processed by a camel route that reads messages from the queue passes them to this class for processing.
 * </p>
 * <p>
 * The status payload is compressed priror
 * </p>
 *
 * @author Richard Lucas
 */
@Named
public class TaskStatusQueue {

    private static final Logger LOGGER = Logger.getLogger(TaskStatusQueue.class);
    private static final String QUEUE_MESSAGE = "{\"taskId\":\"%s\",\"status\":\"%s\"}";

    @Inject
    @Omakase
    CamelQueueEndpoint camelQueueEndpoint;
    @Inject
    @Omakase
    ProducerTemplate producerTemplate;
    @Inject
    @Omakase
    ConsumerTemplate consumerTemplate;
    @Inject
    TaskManager taskManager;
    @Inject
    Tasks tasks;
    @Inject
    OmakaseCluster omakaseCluster;

    /**
     * Adds the task status update to the task status queue.
     *
     * @param taskId
     *         the task id
     * @param taskStatusUpdate
     *         the task status update
     */
    public void add(String taskId, TaskStatusUpdate taskStatusUpdate) {
        producerTemplate.send(getQueueEndpoint(), exchange -> exchange.getIn().setBody(toQueueMessage(taskId, taskStatusUpdate)));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Added task status update for task " + taskId + " to task status queue");
        }
    }

    /**
     * Process the given task status update message.
     *
     * @param queueMessage
     *         the message to process
     */
    public void processQueueMessage(String queueMessage) {
        try (StringReader stringReader = new StringReader(queueMessage); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            String taskId = jsonObject.getString("taskId");
            String status = Compressors.uncompressString(jsonObject.getString("status"));
            taskManager.updateTaskStatus(taskId, tasks.taskStatusUpdateFromJson(taskId, status));
        }
    }

    private static String toQueueMessage(String taskId, TaskStatusUpdate taskStatusUpdate) {
        return String.format(QUEUE_MESSAGE, taskId, Compressors.compressString(taskStatusUpdate.toJson()));
    }

    private String getQueueEndpoint() {
        return camelQueueEndpoint.getQueueEndpoint(omakaseCluster.getClusterName().toLowerCase() + "-task-status-queue");
    }
}
