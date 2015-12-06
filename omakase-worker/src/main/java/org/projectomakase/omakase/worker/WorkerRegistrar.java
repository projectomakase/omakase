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

import com.google.common.base.Strings;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.TailCall;
import org.projectomakase.omakase.commons.functions.TailCalls;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.worker.configuration.WorkerName;
import org.projectomakase.omakase.worker.rest.v1.client.OmakaseClient;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;

/**
 * Registers and un-registers the worker with Omakase.
 *
 * @author Richard Lucas
 */
public class WorkerRegistrar {

    private static final Logger LOGGER = Logger.getLogger(WorkerRegistrar.class);

    @Inject
    OmakaseClient omakaseClient;
    @Inject
    @WorkerName
    String workerName;
    @Inject
    @ConfigProperty(name = "register.max.retries", defaultValue = "3")
    int maxRetries;
    @Inject
    @ConfigProperty(name = "register.retry.frequency.ms", defaultValue = "5000")
    int retryFrequencyInMs;

    /**
     * Registers the worker with Omakase.
     *
     * @return the unique id assigned to the worker.
     */
    public String register() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Registering with Omakase ....");
        }

        String workerId = register(workerName, 0).invoke();
        if (!Strings.isNullOrEmpty(workerId)) {
            LOGGER.info("Registered worker " +  workerName + " with Omakase, id: " + workerId);
            return workerId;
        } else {
            String message = "Failed to register with Omakase";
            LOGGER.error(message);
            throw new OmakaseRuntimeException(message);
        }
    }

    /**
     * Un-registers the worker from Omakase.
     */
    public void unregister() {
        omakaseClient.unregisterWorker();
        LOGGER.info("Unregistered worker from Omakase");
    }

    private TailCall<String> register(String workerName, int retries) {
        if (retries == maxRetries - 1) {
            return TailCalls.done(null);
        }
        try {
            return TailCalls.done(omakaseClient.registerWorker(workerName));
        } catch (OmakaseRuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            Throwables.voidInstance(() -> Thread.sleep(retryFrequencyInMs));
            return TailCalls.call(() -> register(workerName, retries + 1));
        }
    }
}
