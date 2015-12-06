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
import org.projectomakase.omakase.task.api.Task;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

import javax.inject.Inject;
import java.util.Optional;

/**
 * ActiveMQ specific implementation of {@link TaskQueueDelegate}.
 *
 * @author Richard Lucas
 */
public class ActiveMqTaskQueueDelegate implements TaskQueueDelegate {

    private static final String PROVIDER_NAME = "ACTIVEMQ";

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
    JMSPriorityConverter jmsPriorityConverter;
    @Inject
    OmakaseCluster omakaseCluster;

    @Override
    public String getType() {
        return PROVIDER_NAME;
    }

    @Override
    public void add(Task task) {
        String endpoint = getQueueEndpoint();
        producerTemplate.send(endpoint, exchange -> {
            exchange.getIn().setBody(task.getId());
            exchange.getIn().setHeader("JMSPriority", jmsPriorityConverter.convert(task.getPriority()));
            exchange.getIn().setHeader("TaskType", task.getType());
        });
    }

    @Override
    public Optional<String> get(String type) {
        String endpoint = getQueueEndpoint() + "&selector=TaskType='" + type + "'";
        return Optional.ofNullable(consumerTemplate.receiveBody(endpoint, 100, String.class));
    }

    @Override
    public void drain() {
        Optional<String> taskId;
        do {
            taskId = Optional.ofNullable(consumerTemplate.receiveBody(getQueueEndpoint(), 100, String.class));
        } while (taskId.isPresent());
    }

    private String getQueueEndpoint() {
        // pre-fetch size must be 0 when using a consumer template, this ensures the current consumer does not pre-fetch messages hiding them from other concurrent consumers.
        return camelQueueEndpoint.getQueueEndpoint(omakaseCluster.getClusterName().toLowerCase() + "-task-queue").replace("destination.consumer.prefetchSize=1", "destination.consumer.prefetchSize=0");
    }
}
