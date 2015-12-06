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
package org.projectomakase.omakase;

import org.projectomakase.omakase.camel.CamelLifecycle;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.version.Version;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Manages Omakase's lifecycle.
 *
 * @author Richard Lucas
 */
@Singleton
@Startup
public class OmakaseLifecycle {

    private static final Logger LOGGER = Logger.getLogger(OmakaseLifecycle.class);

    @Inject
    CamelLifecycle camelLifecycle;
    @Inject
    Version version;

    @PostConstruct
    public void startup() {
        LOGGER.info("Omakase version: " + version.getVersion() + " (" + version.getBuildTag() + ")");
        Throwables.voidInstance(camelLifecycle::startup);
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down Omakase ....");
        camelLifecycle.shutdown();
    }
}
