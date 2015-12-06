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
package org.projectomakase.omakase.camel;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.camel.routes.TaskStatusQueueRoute;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.spi.ThreadPoolFactory;
import org.apache.camel.spi.ThreadPoolProfile;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Responsible for starting/stopping Apache Camel on application startup/shutdown.
 * <p>
 * Registers configured camel routes.
 *
 * @author Richard Lucas
 */
public class CamelLifecycle {

    private static final Logger LOGGER = Logger.getLogger(CamelLifecycle.class);

    @Inject
    CamelContext camelContext;
    @Inject
    CamelBeanProcessorInterceptor camelBeanProcessorInterceptor;
    @Inject
    @Omakase
    CamelQueueEndpoint camelQueueEndpoint;
    @Inject
    TaskStatusQueueRoute taskStatusQueueRoute;
    @Resource
    ManagedExecutorService managedExecutorService;
    @Resource
    ManagedScheduledExecutorService managedScheduledExecutorService;

    public void startup() {
        try {
            camelContext.setTracing(false);
            camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy("omakase-camel-context"));
            registerInterceptorStrategy();
            registerExecutorService();
            camelQueueEndpoint.register(camelContext);
            registerRoutes();
            camelContext.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start Omakase's Camel Context", e);
        }
    }

    public void shutdown() {
        try {
            camelContext.stop();
        } catch (Exception e) {
            LOGGER.error("Failed to stop Omakase's Camel Context", e);
        }
    }

    private void registerInterceptorStrategy() {
        camelContext.addInterceptStrategy(camelBeanProcessorInterceptor);
    }

    private void registerExecutorService() {
        // Delegates thread management to the Java EE ManagedExecutorService
        ManagedExecutorServiceManager managedExecutorServiceManager = new ManagedExecutorServiceManager(camelContext);
        managedExecutorServiceManager.setThreadPoolFactory(new ThreadPoolFactory() {
            @Override
            public ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
                return managedExecutorService;
            }

            @Override
            public ExecutorService newThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                return managedExecutorService;
            }

            @Override
            public ScheduledExecutorService newScheduledThreadPool(ThreadPoolProfile profile, ThreadFactory threadFactory) {
                return managedScheduledExecutorService;
            }
        });
        camelContext.setExecutorServiceManager(managedExecutorServiceManager);
    }

    private void registerRoutes() {
        Throwables.voidInstance(() -> camelContext.addRoutes(taskStatusQueueRoute));
    }
}
