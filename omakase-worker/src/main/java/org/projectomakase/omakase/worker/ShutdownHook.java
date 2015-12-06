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

import org.jboss.logging.Logger;

/**
 * Remote Tool Container Shutdown Hook. Registers a shutdown hook with the JVM and blocks the main thread to keep the container running. When the shutdown hook is triggered the container is notified
 * and the main thread
 * is released.
 *
 * @author Richard Lucas
 */
class ShutdownHook implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ShutdownHook.class);
    private boolean block = true;
    private final Worker worker;

    /**
     * Instantiates a new ShutdownHook
     *
     * @param worker
     *         the {@link Worker} that should be shutdown when the shutdown hook is triggered
     */
    public ShutdownHook(Worker worker) {
        this.worker = worker;
        Thread shutdownThread = new Thread(this, "RemoteToolContainer - " + getClass().getSimpleName());
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        LOGGER.info("Shutdown hook " + getClass().getSimpleName() + " registered.");
    }

    @Override
    public synchronized void run() {
        LOGGER.info("The Java runtime is going down. Initiating shutdown sequence...");
        try {
            worker.shutdown();
        } catch (Exception ex) {
            LOGGER.error(worker.getClass().getSimpleName() + " failed to shutdown properly.", ex);
        }
        LOGGER.info("Shutdown sequence completed. Notify all waiting threads to exit...");
        block = false;
        notifyAll();
    }

    /**
     * Blocks the applications main thread to ensure it stays alive until the shutdown hook is triggered
     */
    public synchronized void block() {
        LOGGER.info("Blocked on shutdown hook...");
        try {
            while (block) {
                wait();
            }
        } catch (Exception ex) {
            LOGGER.error("Interrupted while blocked on the shutdown hook...", ex);
        }
        LOGGER.info("Unblocked by shutdown hook...");
    }
}
