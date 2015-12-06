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
package org.projectomakase.omakase.worker.configuration;

import org.projectomakase.omakase.commons.functions.Throwables;
import org.apache.deltaspike.core.spi.config.BaseConfigPropertyProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import java.net.Inet4Address;
import java.util.UUID;

/**
 * ConfigProperty producer that returns the workers name, either using the value returned by the specified property name or the default value that is based on the machines host address.
 *
 * @author Richard Lucas
 */
@ApplicationScoped
public class WorkerNameConfigPropertyProducer extends BaseConfigPropertyProducer {

    private static final String DEFAULT_NAME = Throwables.returnableInstance(() -> Inet4Address.getLocalHost().getHostAddress() + "-" + UUID.randomUUID().toString());

    @Produces
    @Dependent
    @WorkerName
    public String produceWorkerName() {
        return getPropertyValue("name", DEFAULT_NAME);
    }
}
