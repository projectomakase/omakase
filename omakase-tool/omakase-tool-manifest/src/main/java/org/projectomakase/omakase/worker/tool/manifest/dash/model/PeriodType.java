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
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PeriodType", propOrder = {
        "baseURLs",
        "segmentBase",
        "segmentList",
        "segmentTemplate",
        "adaptationSets",
        "subsets",
        "anies"
})
public class PeriodType {

    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURLs;
    @XmlElement(name = "SegmentBase")
    protected SegmentBaseType segmentBase;
    @XmlElement(name = "SegmentList")
    protected SegmentListType segmentList;
    @XmlElement(name = "SegmentTemplate")
    protected SegmentTemplateType segmentTemplate;
    @XmlElement(name = "AdaptationSet")
    protected List<AdaptationSetType> adaptationSets;
    @XmlElement(name = "Subset")
    protected List<SubsetType> subsets;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
    protected String href;
    @XmlAttribute(name = "actuate", namespace = "http://www.w3.org/1999/xlink")
    protected ActuateType actuate;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "start")
    protected Duration start;
    @XmlAttribute(name = "duration")
    protected Duration duration;
    @XmlAttribute(name = "bitstreamSwitching")
    protected Boolean bitstreamSwitching;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public List<BaseURLType> getBaseURLs() {
        if (baseURLs == null) {
            baseURLs = new ArrayList<BaseURLType>();
        }
        return this.baseURLs;
    }


    public SegmentBaseType getSegmentBase() {
        return segmentBase;
    }


    public void setSegmentBase(SegmentBaseType value) {
        this.segmentBase = value;
    }


    public SegmentListType getSegmentList() {
        return segmentList;
    }


    public void setSegmentList(SegmentListType value) {
        this.segmentList = value;
    }


    public SegmentTemplateType getSegmentTemplate() {
        return segmentTemplate;
    }


    public void setSegmentTemplate(SegmentTemplateType value) {
        this.segmentTemplate = value;
    }


    public List<AdaptationSetType> getAdaptationSets() {
        if (adaptationSets == null) {
            adaptationSets = new ArrayList<AdaptationSetType>();
        }
        return this.adaptationSets;
    }


    public List<SubsetType> getSubsets() {
        if (subsets == null) {
            subsets = new ArrayList<SubsetType>();
        }
        return this.subsets;
    }


    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }


    public String getHref() {
        return href;
    }


    public void setHref(String value) {
        this.href = value;
    }


    public ActuateType getActuate() {
        if (actuate == null) {
            return ActuateType.ON_REQUEST;
        } else {
            return actuate;
        }
    }


    public void setActuate(ActuateType value) {
        this.actuate = value;
    }


    public String getId() {
        return id;
    }


    public void setId(String value) {
        this.id = value;
    }


    public Duration getStart() {
        return start;
    }


    public void setStart(Duration value) {
        this.start = value;
    }


    public Duration getDuration() {
        return duration;
    }


    public void setDuration(Duration value) {
        this.duration = value;
    }


    public boolean isBitstreamSwitching() {
        if (bitstreamSwitching == null) {
            return false;
        } else {
            return bitstreamSwitching;
        }
    }


    public void setBitstreamSwitching(Boolean value) {
        this.bitstreamSwitching = value;
    }


    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
