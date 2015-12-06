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
package org.projectomakase.omakase.rest.model.v1;

import com.google.common.collect.ImmutableSet;

import javax.jcr.nodetype.PropertyDefinition;

/**
 * @author Richard Lucas
 */
public class TemplateModel {

    private String type;
    private ImmutableSet<PropertyDefinition> properties;

    public TemplateModel() {
        // required to marshal/unmarshal
    }

    public TemplateModel(String type, ImmutableSet<PropertyDefinition> properties) {
        this.type = type;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public ImmutableSet<PropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(ImmutableSet<PropertyDefinition> properties) {
        this.properties = properties;
    }
}
