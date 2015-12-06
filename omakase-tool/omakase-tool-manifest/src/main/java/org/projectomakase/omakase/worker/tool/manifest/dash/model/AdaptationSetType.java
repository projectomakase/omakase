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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AdaptationSetType", propOrder = {
        "accessibilities",
        "roles",
        "ratings",
        "viewpoints",
        "contentComponents",
        "baseURLs",
        "segmentBase",
        "segmentList",
        "segmentTemplate",
        "representations"
})
public class AdaptationSetType
        extends RepresentationBaseType {

    @XmlElement(name = "Accessibility")
    protected List<DescriptorType> accessibilities;
    @XmlElement(name = "Role")
    protected List<DescriptorType> roles;
    @XmlElement(name = "Rating")
    protected List<DescriptorType> ratings;
    @XmlElement(name = "Viewpoint")
    protected List<DescriptorType> viewpoints;
    @XmlElement(name = "ContentComponent")
    protected List<ContentComponentType> contentComponents;
    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURLs;
    @XmlElement(name = "SegmentBase")
    protected SegmentBaseType segmentBase;
    @XmlElement(name = "SegmentList")
    protected SegmentListType segmentList;
    @XmlElement(name = "SegmentTemplate")
    protected SegmentTemplateType segmentTemplate;
    @XmlElement(name = "Representation")
    protected List<RepresentationType> representations;
    @XmlAttribute(name = "href", namespace = "http://www.w3.org/1999/xlink")
    protected String href;
    @XmlAttribute(name = "actuate", namespace = "http://www.w3.org/1999/xlink")
    protected ActuateType actuate;
    @XmlAttribute(name = "id")
    @XmlSchemaType(name = "unsignedInt")
    protected Long id;
    @XmlAttribute(name = "group")
    @XmlSchemaType(name = "unsignedInt")
    protected Long group;
    @XmlAttribute(name = "lang")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String lang;
    @XmlAttribute(name = "contentType")
    protected String contentType;
    @XmlAttribute(name = "par")
    protected String par;
    @XmlAttribute(name = "minBandwidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long minBandwidth;
    @XmlAttribute(name = "maxBandwidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxBandwidth;
    @XmlAttribute(name = "minWidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long minWidth;
    @XmlAttribute(name = "maxWidth")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxWidth;
    @XmlAttribute(name = "minHeight")
    @XmlSchemaType(name = "unsignedInt")
    protected Long minHeight;
    @XmlAttribute(name = "maxHeight")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxHeight;
    @XmlAttribute(name = "minFrameRate")
    protected String minFrameRate;
    @XmlAttribute(name = "maxFrameRate")
    protected String maxFrameRate;
    @XmlAttribute(name = "segmentAlignment")
    protected String segmentAlignment;
    @XmlAttribute(name = "subsegmentAlignment")
    protected String subsegmentAlignment;
    @XmlAttribute(name = "subsegmentStartsWithSAP")
    protected Long subsegmentStartsWithSAP;
    @XmlAttribute(name = "bitstreamSwitching")
    protected Boolean bitstreamSwitching;


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


    public List<ContentComponentType> getContentComponents() {
        if (contentComponents == null) {
            contentComponents = new ArrayList<ContentComponentType>();
        }
        return this.contentComponents;
    }


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


    public List<RepresentationType> getRepresentations() {
        if (representations == null) {
            representations = new ArrayList<RepresentationType>();
        }
        return this.representations;
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


    public Long getId() {
        return id;
    }


    public void setId(Long value) {
        this.id = value;
    }


    public Long getGroup() {
        return group;
    }


    public void setGroup(Long value) {
        this.group = value;
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


    public Long getMinBandwidth() {
        return minBandwidth;
    }


    public void setMinBandwidth(Long value) {
        this.minBandwidth = value;
    }


    public Long getMaxBandwidth() {
        return maxBandwidth;
    }


    public void setMaxBandwidth(Long value) {
        this.maxBandwidth = value;
    }


    public Long getMinWidth() {
        return minWidth;
    }


    public void setMinWidth(Long value) {
        this.minWidth = value;
    }


    public Long getMaxWidth() {
        return maxWidth;
    }


    public void setMaxWidth(Long value) {
        this.maxWidth = value;
    }


    public Long getMinHeight() {
        return minHeight;
    }


    public void setMinHeight(Long value) {
        this.minHeight = value;
    }


    public Long getMaxHeight() {
        return maxHeight;
    }


    public void setMaxHeight(Long value) {
        this.maxHeight = value;
    }


    public String getMinFrameRate() {
        return minFrameRate;
    }


    public void setMinFrameRate(String value) {
        this.minFrameRate = value;
    }


    public String getMaxFrameRate() {
        return maxFrameRate;
    }


    public void setMaxFrameRate(String value) {
        this.maxFrameRate = value;
    }


    public String getSegmentAlignment() {
        if (segmentAlignment == null) {
            return "false";
        } else {
            return segmentAlignment;
        }
    }


    public void setSegmentAlignment(String value) {
        this.segmentAlignment = value;
    }


    public String getSubsegmentAlignment() {
        if (subsegmentAlignment == null) {
            return "false";
        } else {
            return subsegmentAlignment;
        }
    }


    public void setSubsegmentAlignment(String value) {
        this.subsegmentAlignment = value;
    }


    public long getSubsegmentStartsWithSAP() {
        if (subsegmentStartsWithSAP == null) {
            return 0L;
        } else {
            return subsegmentStartsWithSAP;
        }
    }


    public void setSubsegmentStartsWithSAP(Long value) {
        this.subsegmentStartsWithSAP = value;
    }


    public Boolean isBitstreamSwitching() {
        return bitstreamSwitching;
    }


    public void setBitstreamSwitching(Boolean value) {
        this.bitstreamSwitching = value;
    }

}
