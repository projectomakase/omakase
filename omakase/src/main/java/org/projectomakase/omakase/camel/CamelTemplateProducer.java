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
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI Producer methods for starting and stopping application scoped Camel Templates (Producer and Consumer).
 * <p>
 * Templates should be created once by the application and reused (they are thread safe) as per http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html
 * <p>
 * These methods should NOT be called directly the are intended for used by the CDI Framework.
 *
 * @author Richard Lucas
 */
public class CamelTemplateProducer {

    private static final Logger LOGGER = Logger.getLogger(CamelTemplateProducer.class);

    @Inject
    CamelContext camelContext;

    /**
     * Creates and starts an application scoped ProducerTemplate that has a default cache size of 1000 and is not associated with any endpoint.
     * <p>
     * This CDI Producer is preferred over camel-cdi Producer that creates a ProducerTemplate for a specific Endpoint/Camel Context which limits it's reuse.
     *
     * @return an application scope ProducerTemplate that has a default cache size of 1000 and is not associated with any endpoint.
     */
    @Produces
    @Omakase
    @ApplicationScoped
    public ProducerTemplate getProducerTemplate() {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        LOGGER.info("Created Camel Producer Template with max cache size " + producerTemplate.getMaximumCacheSize());
        return producerTemplate;
    }

    /**
     * Stops the ProducerTemplate when it is disposed.
     *
     * @param producerTemplate
     *         the producerTemplate to stop
     * @throws Exception
     *         if the producer fails to stop
     */
    public void stopProducerTemplate(@Disposes @Omakase ProducerTemplate producerTemplate) throws Exception {
        producerTemplate.stop();
        LOGGER.info("Stopped Camel Producer Template");
    }

    /**
     * Creates and starts an application scope ConsumeTemplate that has a default cache size of 1000 and is not associated with any endpoint.
     *
     * @return an application scope ConsumeTemplate that has a default cache size of 1000 and is not associated with any endpoint.
     */
    @Produces
    @Omakase
    @ApplicationScoped
    public ConsumerTemplate getConsumerTemplate() {
        ConsumerTemplate consumerTemplate = camelContext.createConsumerTemplate(1);
        LOGGER.info("Created Camel Consumer Template with max cache size " + consumerTemplate.getMaximumCacheSize());
        return consumerTemplate;
    }

    /**
     * Stops the ConsumerTemplate when it is disposed.
     *
     * @param consumerTemplate
     *         the consumerTemplate to stop
     * @throws Exception
     *         if the consumer fails to stop
     */
    public void stopConsumerTemplate(@Disposes @Omakase ConsumerTemplate consumerTemplate) throws Exception {
        consumerTemplate.stop();
        LOGGER.info("Stopped Camel Consumer Template");
    }
}
