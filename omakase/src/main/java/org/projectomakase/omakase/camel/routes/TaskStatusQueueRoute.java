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
package org.projectomakase.omakase.camel.routes;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.OmakaseCluster;
import org.projectomakase.omakase.camel.CamelQueueEndpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;

/**
 * Camel route used to consume Task Status Update messages from the Task Status Queue and process them.
 *
 * @author Richard Lucas
 */
public class TaskStatusQueueRoute extends RouteBuilder {

    private static final Logger LOGGER = Logger.getLogger(TaskStatusQueueRoute.class);

    @Inject
    @Omakase
    CamelQueueEndpoint camelQueueEndpoint;
    @Inject
    OmakaseCluster omakaseCluster;
    @Inject
    @ConfigProperty(name = "omakase.task.status.queue.throttle.max.per.period")
    int throttleMaxPerPeriod;
    @Inject
    @ConfigProperty(name = "omakase.task.status.queue.throttle.period.ms")
    int periodInMs;

    @Override
    public void configure() throws Exception {
        // the route is NOT transacted and messages are consumed from the queue regardless of the outcome of processing them. This is to ensure that failed messages do not clog up the queue.

        // this ensures the message is consumed even on error.
        onException(Exception.class).continued(true).to("log:org.projectomakase.omakase.camel.routes.TaskStatusQueueRoute?level=ERROR");

        RouteDefinition routeDefinition = from(camelQueueEndpoint.getQueueEndpoint(omakaseCluster.getClusterName() + "-task-status-queue")).routeId("TaskStatusQueueRoute");

        if (throttleMaxPerPeriod > 0) {
            LOGGER.info("Throttling task-status-queue route to " + throttleMaxPerPeriod  + " message(s) per " + periodInMs + " ms");
            routeDefinition.throttle(throttleMaxPerPeriod).timePeriodMillis(periodInMs).beanRef("taskStatusQueue", "processQueueMessage");
        } else {
            routeDefinition.beanRef("taskStatusQueue", "processQueueMessage");
        }
    }
}
