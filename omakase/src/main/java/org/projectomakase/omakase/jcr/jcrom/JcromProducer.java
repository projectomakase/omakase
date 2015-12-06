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
package org.projectomakase.omakase.jcr.jcrom;

import org.projectomakase.omakase.Omakase;
import org.jcrom.Jcrom;
import org.jcrom.annotations.JcrNode;
import org.reflections.Reflections;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Set;

/**
 * @author Richard Lucas
 */
public class JcromProducer {

    @Inject
    Reflections reflections;

    @Produces
    @Omakase
    @ApplicationScoped
    public Jcrom jcrom() {
        Jcrom jcrom = new Jcrom(true, true);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(JcrNode.class, true);
        annotated.forEach(annotatedClass -> jcrom.map(annotatedClass));
        return jcrom;
    }
}
