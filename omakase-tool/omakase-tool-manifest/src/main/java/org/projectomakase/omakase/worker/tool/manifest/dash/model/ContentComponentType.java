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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContentComponentType", propOrder = {
        "accessibilities",
        "roles",
        "ratings",
        "viewpoints",
        "anies"
})
public class ContentComponentType {

    @XmlElement(name = "Accessibility")
    protected List<DescriptorType> accessibilities;
    @XmlElement(name = "Role")
    protected List<DescriptorType> roles;
    @XmlElement(name = "Rating")
    protected List<DescriptorType> ratings;
    @XmlElement(name = "Viewpoint")
    protected List<DescriptorType> viewpoints;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "id")
    @XmlSchemaType(name = "unsignedInt")
    protected Long id;
    @XmlAttribute(name = "lang")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String lang;
    @XmlAttribute(name = "contentType")
    protected String contentType;
    @XmlAttribute(name = "par")
    protected String par;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    
    public List<DescriptorType> getAccessibilities() {
        if (accessibilities == null) {
            accessibilities = new ArrayList<DescriptorType>();
        }
        return this.accessibilities;
    }

    
    public List<DescriptorType> getRoles() {
        if (roles == null) {
            roles = new ArrayList<DescriptorType>();
        }
        return this.roles;
    }

    
    public List<DescriptorType> getRatings() {
        if (ratings == null) {
            ratings = new ArrayList<DescriptorType>();
        }
        return this.ratings;
    }

    
    public List<DescriptorType> getViewpoints() {
        if (viewpoints == null) {
            viewpoints = new ArrayList<DescriptorType>();
        }
        return this.viewpoints;
    }

    
    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }

    
    public Long getId() {
        return id;
    }

    
    public void setId(Long value) {
        this.id = value;
    }

    
    public String getLang() {
        return lang;
    }

    
    public void setLang(String value) {
        this.lang = value;
    }

    
    public String getContentType() {
        return contentType;
    }

    
    public void setContentType(String value) {
        this.contentType = value;
    }

    
    public String getPar() {
        return par;
    }

    
    public void setPar(String value) {
        this.par = value;
    }

    
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
