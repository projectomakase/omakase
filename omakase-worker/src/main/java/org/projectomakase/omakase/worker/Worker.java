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

import org.projectomakase.omakase.worker.camel.CamelActivator;
import org.projectomakase.omakase.worker.tool.Tool;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.api.provider.DependentProvider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Omakase Worker.
 * <p>
 * Starts the CDI container and activates the camel routes used to consume and produce tool messages via the Omakase REST API.
 * </p>
 *
 * @author Richard Lucas
 */
public class Worker {

    private static final Logger LOGGER = Logger.getLogger(Worker.class);

    private final CdiContainer cdiContainer = new CdiContainer();

    private CamelActivator camelActivator;

    public static void main(String[] args) {
        Worker worker = new Worker();
        try {
            ShutdownHook shutdownHook = new ShutdownHook(worker);
            worker.start();
            shutdownHook.block();
        } catch (Exception e) {
            LOGGER.error(worker.getClass().getSimpleName() + " failed to start.", e);
            // Force the shutdown to occur, this doesn't always happen automatically when an error occurs during startup
            System.exit(1);
        }
    }

    public void start() {
        cdiContainer.start();
        DependentProvider<WorkerRegistrar> workerRegistrarProvider = BeanProvider.getDependent(WorkerRegistrar.class);
        try {
            workerRegistrarProvider.get().register();
            startCamel();
        } catch(Exception e) {
            LOGGER.error("Unable to start worker.  Reason:" + e.getMessage(), e);
            shutdown();
        } finally {
            workerRegistrarProvider.destroy();
        }
    }

    public void shutdown() {
        DependentProvider<WorkerRegistrar> workerRegistrarProvider = BeanProvider.getDependent(WorkerRegistrar.class);
        try {
            stopCamel();
            workerRegistrarProvider.get().unregister();
        } finally {
            workerRegistrarProvider.destroy();
        }
        cdiContainer.stop();
    }

    private void startCamel() {
        camelActivator = BeanProvider.getContextualReference(CamelActivator.class, false);

        // Register a route for each tool on the class path
        List<Tool> tools = BeanProvider.getContextualReferences(Tool.class, true);
        camelActivator.registerRoutes(tools);

        camelActivator.start();
    }

    private void stopCamel() {
        if (camelActivator != null) {
            camelActivator.stop();
        }
    }
}
