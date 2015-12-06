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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * CDI Extension used to getTaskGroupStatus 'Ambiguous dependencies for type CdiCamelContext'.
 * <p>
 * See https://issues.apache.org/jira/browse/CAMEL-7760 for additional information.
 * </p>
 *
 * @author Richard Lucas
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public class CamelCdiVetoExtension implements Extension {

    void interceptProcessAnnotatedTypes(@Observes ProcessAnnotatedType processAnnotatedType) {
        if ("org.apache.camel.cdi.CdiCamelContext".equals(processAnnotatedType.getAnnotatedType().getJavaClass().getName())) {
            processAnnotatedType.veto();
        }
    }
}
