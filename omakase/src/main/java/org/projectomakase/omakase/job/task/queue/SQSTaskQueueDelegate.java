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

import com.google.common.base.CaseFormat;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.OmakaseCluster;
import org.projectomakase.omakase.camel.CamelQueueEndpoint;
import org.projectomakase.omakase.job.task.Tasks;
import org.projectomakase.omakase.task.api.Task;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

import javax.inject.Inject;
import java.util.Optional;

/**
 * SQS specific implementation of {@link TaskQueueDelegate}.
 *
 * @author Richard Lucas
 */
public class SQSTaskQueueDelegate implements TaskQueueDelegate {

    private static final String PROVIDER_NAME = "SQS";

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
    @Inject
    Tasks tasks;

    @Override
    public String getType() {
        return PROVIDER_NAME;
    }

    @Override
    public void add(Task task) {
        producerTemplate.send(getEndpoint(task.getType()), exchange -> exchange.getIn().setBody(task.getId()));
    }

    @Override
    public Optional<String> get(String type) {
        return Optional.ofNullable(consumerTemplate.receiveBody(getEndpoint(type), 5000, String.class));
    }

    private String getEndpoint(String taskType) {
        String type = CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_HYPHEN).convert(taskType);
        return camelQueueEndpoint.getQueueEndpoint(omakaseCluster.getClusterName().toLowerCase() + "-" + type + "-task-queue");
    }

    @Override
    public void drain() {
        // no-op, too slow
    }
}
