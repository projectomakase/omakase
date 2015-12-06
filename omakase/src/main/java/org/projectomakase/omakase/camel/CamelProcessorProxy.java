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

import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.security.OmakaseSecurity;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logging.Logger;

import javax.ejb.Stateless;

/**
 * Proxies the execution of camel processors, running them as the Omakase System user.
 * <p>
 * The proxy is implemented as an EJB to ensure that processor is executed with the scope Java EE Request Context (required by Omakase's security framework).
 * </p>
 *
 * @author Richard Lucas
 */
@Stateless
public class CamelProcessorProxy {

    private static final Logger LOGGER = Logger.getLogger(CamelProcessorProxy.class);

    public void doAsSystem(Processor processor, Exchange exchange) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Proxying Camel Bean Processor as System User");
        }
        Throwables.voidInstance(() -> OmakaseSecurity.doAsSystem(() -> {
            processor.process(exchange);
            return true;
        }));
    }
}
