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
package org.projectomakase.omakase.worker.tool.protocol;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier used to resolve the {@link ProtocolHandler} implementation that should be used for a given protocol.
 *
 * @author Richard Lucas
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, TYPE, PARAMETER, FIELD})
public @interface HandleProtocol {

    /**
     * Returns the protocol support by the protocol handler e.g. file
     *
     * @return the protocol support by the protocol handler e.g. file
     */
    String value();

    @SuppressWarnings("ClassExplicitlyAnnotation")
    public class HandleProtocolAnnotationLiteral extends AnnotationLiteral<HandleProtocol> implements HandleProtocol {

        private String value;

        public HandleProtocolAnnotationLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
