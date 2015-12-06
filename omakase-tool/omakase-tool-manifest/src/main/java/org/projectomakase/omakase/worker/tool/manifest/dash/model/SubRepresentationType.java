/*
 * #%L
 * omakase-tool-manifest
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
package org.projectomakase.omakase.worker.tool.manifest.dash.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubRepresentationType")
public class SubRepresentationType
        extends RepresentationBaseType {

    @XmlAttribute(name = "level")
    @XmlSchemaType(name = "unsignedInt")
    protected Long level;
    @XmlAttribute(name = "dependencyLevel")
    protected List<Long> dependencyLevels;
    @XmlAttribute(name = "bandwidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long bandwidth;
    @XmlAttribute(name = "contentComponent")
    protected List<String> contentComponents;


    public Long getLevel() {
        return level;
    }


    public void setLevel(Long value) {
        this.level = value;
    }


    public List<Long> getDependencyLevels() {
        if (dependencyLevels == null) {
            dependencyLevels = new ArrayList<Long>();
        }
        return this.dependencyLevels;
    }


    public Long getBandwidth() {
        return bandwidth;
    }


    public void setBandwidth(Long value) {
        this.bandwidth = value;
    }


    public List<String> getContentComponents() {
        if (contentComponents == null) {
            contentComponents = new ArrayList<String>();
        }
        return this.contentComponents;
    }

}
