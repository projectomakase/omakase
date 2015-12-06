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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

/**
 * Camel Route responsible for requesting tasks from Omakase and delegating them to the correct tool.
 *
 * @author Richard Lucas
 */
public class RequestTasksRoute extends RouteBuilder {

    private final int pollFrequencyInSecs;

    public RequestTasksRoute(CamelContext camelContext, int pollFrequencyInSecs) {
        super(camelContext);
        this.pollFrequencyInSecs = pollFrequencyInSecs;
    }

    @Override
    public void configure() throws Exception {
        from("timer://requestTasks?delay=10000&period=" + (pollFrequencyInSecs * 1000)).routeId("request_tasks").beanRef("taskRequester");
    }
}
