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
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentTemplateType")
public class SegmentTemplateType
        extends MultipleSegmentBaseType {

    @XmlAttribute(name = "media")
    protected String media;
    @XmlAttribute(name = "index")
    protected String index;
    @XmlAttribute(name = "initialization")
    protected String initializationAttribute;
    @XmlAttribute(name = "bitstreamSwitching")
    protected String bitstreamSwitchingAttribute;


    public String getMedia() {
        return media;
    }


    public void setMedia(String value) {
        this.media = value;
    }


    public String getIndex() {
        return index;
    }


    public void setIndex(String value) {
        this.index = value;
    }


    public String getInitializationAttribute() {
        return initializationAttribute;
    }


    public void setInitializationAttribute(String value) {
        this.initializationAttribute = value;
    }


    public String getBitstreamSwitchingAttribute() {
        return bitstreamSwitchingAttribute;
    }


    public void setBitstreamSwitchingAttribute(String value) {
        this.bitstreamSwitchingAttribute = value;
    }

}
