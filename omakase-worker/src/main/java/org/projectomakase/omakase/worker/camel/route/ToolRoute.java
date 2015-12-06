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
package org.projectomakase.omakase.worker.camel.route;

import org.projectomakase.omakase.worker.tool.ToolInfo;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

/**
 * Builds a Camel Route that can be used to directly execute a tool.
 *
 * @author Richard Lucas
 */
public class ToolRoute extends RouteBuilder {

    private final ToolInfo toolInfo;

    public ToolRoute(CamelContext camelContext, ToolInfo toolInfo) {
        super(camelContext);
        this.toolInfo = toolInfo;
    }

    @Override
    public void configure() throws Exception {
        String routeId = toolInfo.getName().toLowerCase() + "_tool";
        from("seda://" + toolInfo.getName() + "?concurrentConsumers=" + toolInfo.getMaxCapacity()).routeId(routeId).errorHandler(loggingErrorHandler())
                .beanRef(toolInfo.getName(), "execute");
    }
}
