/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker;

import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.worker.rest.v1.client.OmakaseClient;
import org.projectomakase.omakase.worker.tool.ToolRegistry;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * @author Richard Lucas
 */
@Named
public class TaskRequester {

    private static final Logger LOGGER = Logger.getLogger(TaskRequester.class);

    @Inject
    ToolRegistry toolRegistry;
    @Inject
    OmakaseClient omakaseClient;
    @Inject
    @Omakase
    ProducerTemplate producerTemplate;

    public void requestTasks() {
        List<Task> tasks = omakaseClient.consumeToolTasks(toolRegistry.getAvailableCapacity());

        if (LOGGER.isDebugEnabled() && !tasks.isEmpty()) {
            LOGGER.debug("Received " + tasks.size() + " tasks");
        }

        tasks.forEach(task -> {
            toolRegistry.decreaseAvailableCapacity(task.getType());
            producerTemplate.send("seda:" + task.getType(), exchange -> exchange.getIn().setBody(task));
        });
    }
}
