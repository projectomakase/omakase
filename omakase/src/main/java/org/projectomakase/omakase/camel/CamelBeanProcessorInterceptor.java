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

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.spi.InterceptStrategy;

import javax.inject.Inject;

/**
 * Camel Intercept Strategy that intercepts bean processors and executes them using a proxy.
 *
 * @author Richard Lucas
 */
public class CamelBeanProcessorInterceptor implements InterceptStrategy {

    @Inject
    CamelProcessorProxy processorProxy;

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext context, ProcessorDefinition<?> definition, Processor target, Processor nextTarget) throws Exception {
        if ("bean".equals(definition.getShortName())) {
            return new DelegateAsyncProcessor(exchange -> processorProxy.doAsSystem(target, exchange));
        } else {
            return target;
        }
    }
}
