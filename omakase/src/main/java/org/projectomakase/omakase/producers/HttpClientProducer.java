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
package org.projectomakase.omakase.producers;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.http.HttpClientFactory;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Apache {@link HttpClient} 4.2.6 CDI Producer
 *
 * @author Richard Lucas
 */
public class HttpClientProducer {

    private static final Logger LOGGER = Logger.getLogger(HttpClientProducer.class);

    @Inject
    @ConfigProperty(name = "http.client.max.connections")
    int maxConnections;

    @Inject
    @ConfigProperty(name = "http.client.connection.timeout.ms")
    int connectionTimeoutInMs;

    @Inject
    @ConfigProperty(name = "http.client.socket.timeout.ms")
    int socketTimeoutInMs;

    @Produces
    @ApplicationScoped
    @Omakase
    public HttpClient getHttpClient() {
        return HttpClientFactory.pooledConnectionHttpClient(maxConnections, connectionTimeoutInMs, socketTimeoutInMs);
    }

    public void close(@Disposes @Omakase HttpClient httpClient) {
        httpClient.getConnectionManager().shutdown();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Shutdown Pooled HTTP Client");
        }
    }
}
