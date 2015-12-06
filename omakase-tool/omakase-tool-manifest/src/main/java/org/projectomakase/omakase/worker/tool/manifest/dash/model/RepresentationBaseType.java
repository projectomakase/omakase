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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RepresentationBaseType", propOrder = {
        "framePackings",
        "audioChannelConfigurations",
        "contentProtections",
        "essentialProperties",
        "supplementalProperties",
        "anies"
})
@XmlSeeAlso({
        AdaptationSetType.class,
        SubRepresentationType.class,
        RepresentationType.class
})
public class RepresentationBaseType {

    @XmlElement(name = "FramePacking")
    protected List<DescriptorType> framePackings;
    @XmlElement(name = "AudioChannelConfiguration")
    protected List<DescriptorType> audioChannelConfigurations;
    @XmlElement(name = "ContentProtection")
    protected List<DescriptorType> contentProtections;
    @XmlElement(name = "EssentialProperty")
    protected List<DescriptorType> essentialProperties;
    @XmlElement(name = "SupplementalProperty")
    protected List<DescriptorType> supplementalProperties;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "profiles")
    protected String profiles;
    @XmlAttribute(name = "width")
    @XmlSchemaType(name = "unsignedInt")
    protected Long width;
    @XmlAttribute(name = "height")
    @XmlSchemaType(name = "unsignedInt")
    protected Long height;
    @XmlAttribute(name = "sar")
    protected String sar;
    @XmlAttribute(name = "frameRate")
    protected String frameRate;
    @XmlAttribute(name = "audioSamplingRate")
    protected String audioSamplingRate;
    @XmlAttribute(name = "mimeType")
    protected String mimeType;
    @XmlAttribute(name = "segmentProfiles")
    protected String segmentProfiles;
    @XmlAttribute(name = "codecs")
    protected String codecs;
    @XmlAttribute(name = "maximumSAPPeriod")
    protected Double maximumSAPPeriod;
    @XmlAttribute(name = "startWithSAP")
    protected Long startWithSAP;
    @XmlAttribute(name = "maxPlayoutRate")
    protected Double maxPlayoutRate;
    @XmlAttribute(name = "codingDependency")
    protected Boolean codingDependency;
    @XmlAttribute(name = "scanType")
    protected VideoScanType scanType;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    public List<DescriptorType> getFramePackings() {
        if (framePackings == null) {
            framePackings = new ArrayList<DescriptorType>();
        }
        return this.framePackings;
    }

    public List<DescriptorType> getAudioChannelConfigurations() {
        if (audioChannelConfigurations == null) {
            audioChannelConfigurations = new ArrayList<DescriptorType>();
        }
        return this.audioChannelConfigurations;
    }

    public List<DescriptorType> getContentProtections() {
        if (contentProtections == null) {
            contentProtections = new ArrayList<DescriptorType>();
        }
        return this.contentProtections;
    }

    public List<DescriptorType> getEssentialProperties() {
        if (essentialProperties == null) {
            essentialProperties = new ArrayList<DescriptorType>();
        }
        return this.essentialProperties;
    }

    public List<DescriptorType> getSupplementalProperties() {
        if (supplementalProperties == null) {
            supplementalProperties = new ArrayList<DescriptorType>();
        }
        return this.supplementalProperties;
    }

    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String value) {
        this.profiles = value;
    }

    public Long getWidth() {
        return width;
    }

    public void setWidth(Long value) {
        this.width = value;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long value) {
        this.height = value;
    }

    public String getSar() {
        return sar;
    }

    public void setSar(String value) {
        this.sar = value;
    }

    public String getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(String value) {
        this.frameRate = value;
    }

    public String getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public void setAudioSamplingRate(String value) {
        this.audioSamplingRate = value;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String value) {
        this.mimeType = value;
    }

    public String getSegmentProfiles() {
        return segmentProfiles;
    }

    public void setSegmentProfiles(String value) {
        this.segmentProfiles = value;
    }

    public String getCodecs() {
        return codecs;
    }

    public void setCodecs(String value) {
        this.codecs = value;
    }

    public Double getMaximumSAPPeriod() {
        return maximumSAPPeriod;
    }

    public void setMaximumSAPPeriod(Double value) {
        this.maximumSAPPeriod = value;
    }

    public Long getStartWithSAP() {
        return startWithSAP;
    }

    public void setStartWithSAP(Long value) {
        this.startWithSAP = value;
    }

    public Double getMaxPlayoutRate() {
        return maxPlayoutRate;
    }

    public void setMaxPlayoutRate(Double value) {
        this.maxPlayoutRate = value;
    }

    public Boolean isCodingDependency() {
        return codingDependency;
    }

    public void setCodingDependency(Boolean value) {
        this.codingDependency = value;
    }

    public VideoScanType getScanType() {
        return scanType;
    }

    public void setScanType(VideoScanType value) {
        this.scanType = value;
    }

    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
