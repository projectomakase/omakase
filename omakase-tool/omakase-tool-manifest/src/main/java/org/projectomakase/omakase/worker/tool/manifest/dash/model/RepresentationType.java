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
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RepresentationType", propOrder = {
        "baseURLs",
        "subRepresentations",
        "segmentBase",
        "segmentList",
        "segmentTemplate"
})
public class RepresentationType
        extends RepresentationBaseType {

    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURLs;
    @XmlElement(name = "SubRepresentation")
    protected List<SubRepresentationType> subRepresentations;
    @XmlElement(name = "SegmentBase")
    protected SegmentBaseType segmentBase;
    @XmlElement(name = "SegmentList")
    protected SegmentListType segmentList;
    @XmlElement(name = "SegmentTemplate")
    protected SegmentTemplateType segmentTemplate;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "bandwidth", required = true)
    @XmlSchemaType(name = "unsignedInt")
    protected long bandwidth;
    @XmlAttribute(name = "qualityRanking")
    @XmlSchemaType(name = "unsignedInt")
    protected Long qualityRanking;
    @XmlAttribute(name = "dependencyId")
    protected List<String> dependencyIds;
    @XmlAttribute(name = "mediaStreamStructureId")
    protected List<String> mediaStreamStructureIds;


    public List<BaseURLType> getBaseURLs() {
        if (baseURLs == null) {
            baseURLs = new ArrayList<BaseURLType>();
        }
        return this.baseURLs;
    }


    public List<SubRepresentationType> getSubRepresentations() {
        if (subRepresentations == null) {
            subRepresentations = new ArrayList<SubRepresentationType>();
        }
        return this.subRepresentations;
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


    public String getId() {
        return id;
    }


    public void setId(String value) {
        this.id = value;
    }


    public long getBandwidth() {
        return bandwidth;
    }


    public void setBandwidth(long value) {
        this.bandwidth = value;
    }


    public Long getQualityRanking() {
        return qualityRanking;
    }


    public void setQualityRanking(Long value) {
        this.qualityRanking = value;
    }


    public List<String> getDependencyIds() {
        if (dependencyIds == null) {
            dependencyIds = new ArrayList<String>();
        }
        return this.dependencyIds;
    }


    public List<String> getMediaStreamStructureIds() {
        if (mediaStreamStructureIds == null) {
            mediaStreamStructureIds = new ArrayList<String>();
        }
        return this.mediaStreamStructureIds;
    }

}
