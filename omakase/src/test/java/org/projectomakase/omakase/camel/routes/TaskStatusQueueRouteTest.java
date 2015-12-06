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

import org.projectomakase.omakase.OmakaseCluster;
import org.projectomakase.omakase.camel.CamelQueueEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ThrottleDefinition;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class TaskStatusQueueRouteTest {

    TaskStatusQueueRoute route;

    @Before
    public void setUp() throws Exception {
        route = new TaskStatusQueueRoute();
        CamelQueueEndpoint camelQueueEndpoint = mock(CamelQueueEndpoint.class);
        doReturn("test").when(camelQueueEndpoint).getQueueEndpoint(anyString());
        OmakaseCluster omakaseCluster = mock(OmakaseCluster.class);
        doReturn("test").when(omakaseCluster).getClusterName();
        route.camelQueueEndpoint = camelQueueEndpoint;
        route.omakaseCluster = omakaseCluster;
    }

    @Test
    public void shouldConfigureRouteWithoutThrottling() throws Exception {
        route.throttleMaxPerPeriod = -1;
        route.periodInMs = 100;
        route.configure();
        RouteDefinition routeDefinition = route.getRouteCollection().getRoutes().get(0);
        assertThat(routeDefinition.getOutputs().stream().filter(processorDefinition -> processorDefinition instanceof ThrottleDefinition).findFirst()).isEmpty();
    }

    @Test
    public void shouldConfigureRouteWithThrottling() throws Exception {
        route.throttleMaxPerPeriod = 10;
        route.periodInMs = 100;
        route.configure();
        RouteDefinition routeDefinition = route.getRouteCollection().getRoutes().get(0);
        ThrottleDefinition throttleDefinition =
                (ThrottleDefinition) routeDefinition.getOutputs().stream().filter(processorDefinition -> processorDefinition instanceof ThrottleDefinition).findFirst().get();
        assertThat(throttleDefinition.getExpression().getExpressionValue().toString()).isEqualTo("10");
        assertThat(throttleDefinition.getTimePeriodMillis()).isEqualTo(100);
    }
}