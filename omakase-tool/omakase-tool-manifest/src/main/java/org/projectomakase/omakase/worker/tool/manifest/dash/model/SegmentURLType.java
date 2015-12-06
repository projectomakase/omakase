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

import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentURLType", propOrder = {
        "anies"
})
public class SegmentURLType {

    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "media")
    @XmlSchemaType(name = "anyURI")
    protected String media;
    @XmlAttribute(name = "mediaRange")
    protected String mediaRange;
    @XmlAttribute(name = "index")
    @XmlSchemaType(name = "anyURI")
    protected String index;
    @XmlAttribute(name = "indexRange")
    protected String indexRange;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    
    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }

    
    public String getMedia() {
        return media;
    }

    
    public void setMedia(String value) {
        this.media = value;
    }

    
    public String getMediaRange() {
        return mediaRange;
    }

    
    public void setMediaRange(String value) {
        this.mediaRange = value;
    }

    
    public String getIndex() {
        return index;
    }

    
    public void setIndex(String value) {
        this.index = value;
    }

    
    public String getIndexRange() {
        return indexRange;
    }

    
    public void setIndexRange(String value) {
        this.indexRange = value;
    }

    
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
