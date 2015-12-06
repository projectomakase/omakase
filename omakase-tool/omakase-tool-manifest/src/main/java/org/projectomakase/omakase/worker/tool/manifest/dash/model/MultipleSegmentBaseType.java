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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultipleSegmentBaseType", propOrder = {
        "segmentTimeline",
        "bitstreamSwitching"
})
@XmlSeeAlso({
        SegmentTemplateType.class,
        SegmentListType.class
})
public class MultipleSegmentBaseType
        extends SegmentBaseType {

    @XmlElement(name = "SegmentTimeline")
    protected SegmentTimelineType segmentTimeline;
    @XmlElement(name = "BitstreamSwitching")
    protected URLType bitstreamSwitching;
    @XmlAttribute(name = "duration")
    @XmlSchemaType(name = "unsignedInt")
    protected Long duration;
    @XmlAttribute(name = "startNumber")
    @XmlSchemaType(name = "unsignedInt")
    protected Long startNumber;


    public SegmentTimelineType getSegmentTimeline() {
        return segmentTimeline;
    }


    public void setSegmentTimeline(SegmentTimelineType value) {
        this.segmentTimeline = value;
    }


    public URLType getBitstreamSwitching() {
        return bitstreamSwitching;
    }


    public void setBitstreamSwitching(URLType value) {
        this.bitstreamSwitching = value;
    }


    public Long getDuration() {
        return duration;
    }


    public void setDuration(Long value) {
        this.duration = value;
    }


    public Long getStartNumber() {
        return startNumber;
    }


    public void setStartNumber(Long value) {
        this.startNumber = value;
    }

}
