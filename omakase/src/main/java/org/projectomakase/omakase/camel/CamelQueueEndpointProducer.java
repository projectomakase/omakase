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
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.stream.StreamSupport;

/**
 * CDI Producer for {@link CamelQueueEndpoint}.
 *
 * @author Richard Lucas
 */
public class CamelQueueEndpointProducer {

    private static final Logger LOGGER = Logger.getLogger(CamelQueueEndpointProducer.class);

    @Inject
    @ConfigProperty(name = "omakase.queue.provider", defaultValue = "ACTIVEMQ")
    String provider;
    @Inject
    Instance<CamelQueueEndpoint> queues;


    @Produces
    @Omakase
    @ApplicationScoped
    public CamelQueueEndpoint getQueueProvider() {
        CamelQueueEndpoint camelQueueEndpoint = StreamSupport.stream(queues.spliterator(), false).filter(q -> provider.equals(q.getProviderName())).findFirst()
                .orElseThrow(() -> new OmakaseRuntimeException("unsupported queue provider " + provider));

        LOGGER.info("Camel queue endpoint provider: " + camelQueueEndpoint.getProviderName());

        return camelQueueEndpoint;
    }
}
