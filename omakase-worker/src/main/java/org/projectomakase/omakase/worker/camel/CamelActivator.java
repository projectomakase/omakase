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
package org.projectomakase.omakase.worker.camel;

import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.worker.camel.route.RequestTasksRoute;
import org.projectomakase.omakase.worker.camel.route.ToolCallbackRoute;
import org.projectomakase.omakase.worker.camel.route.ToolRoute;
import org.projectomakase.omakase.worker.tool.Tool;
import org.projectomakase.omakase.worker.tool.ToolInfo;
import org.projectomakase.omakase.worker.tool.ToolRegistry;
import org.apache.camel.CamelContext;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Richard Lucas
 */
@ApplicationScoped
public class CamelActivator {

    private static final Logger LOGGER = Logger.getLogger(CamelActivator.class);

    @Inject
    CamelContext context;
    @Inject
    ToolRegistry toolRegistry;
    @Inject
    @ConfigProperty(name = "tool.poll.frequency.in.secs")
    int pollFrequencyInSecs;

    public void start() {
        LOGGER.info("Creating CamelContext and registering Camel Routes.");
        try {
            context.setTracing(false);
            context.start();
            LOGGER.info("CamelContext created and camel routes started.");
        } catch (Exception e) {
            LOGGER.error("Failed to create CamelContext and register routes", e);
        }
    }

    public void stop() {
        try {
            context.stop();
        } catch (Exception e) {
            LOGGER.error("Failed to stop CamelContext", e);
        }
        LOGGER.info("CamelContext stopped.");
    }

    public void registerRoutes(List<Tool> tools) {
        List<ToolInfo> toolInfos = toolRegistry.registerTools(tools);
        toolInfos.forEach(toolInfo -> Throwables.voidInstance(() -> context.addRoutes(new ToolRoute(context, toolInfo))));
        Throwables.voidInstance(() -> context.addRoutes(new RequestTasksRoute(context, pollFrequencyInSecs)));
        Throwables.voidInstance(() -> context.addRoutes(new ToolCallbackRoute(context)));
    }
}
