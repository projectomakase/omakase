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
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentBaseType", propOrder = {
        "initialization",
        "representationIndex",
        "anies"
})
@XmlSeeAlso({
        MultipleSegmentBaseType.class
})
public class SegmentBaseType {

    @XmlElement(name = "Initialization")
    protected URLType initialization;
    @XmlElement(name = "RepresentationIndex")
    protected URLType representationIndex;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "timescale")
    @XmlSchemaType(name = "unsignedInt")
    protected Long timescale;
    @XmlAttribute(name = "presentationTimeOffset")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger presentationTimeOffset;
    @XmlAttribute(name = "timeShiftBufferDepth")
    protected Duration timeShiftBufferDepth;
    @XmlAttribute(name = "indexRange")
    protected String indexRange;
    @XmlAttribute(name = "indexRangeExact")
    protected Boolean indexRangeExact;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public URLType getInitialization() {
        return initialization;
    }


    public void setInitialization(URLType value) {
        this.initialization = value;
    }


    public URLType getRepresentationIndex() {
        return representationIndex;
    }


    public void setRepresentationIndex(URLType value) {
        this.representationIndex = value;
    }


    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }


    public Long getTimescale() {
        return timescale;
    }


    public void setTimescale(Long value) {
        this.timescale = value;
    }


    public BigInteger getPresentationTimeOffset() {
        return presentationTimeOffset;
    }


    public void setPresentationTimeOffset(BigInteger value) {
        this.presentationTimeOffset = value;
    }


    public Duration getTimeShiftBufferDepth() {
        return timeShiftBufferDepth;
    }


    public void setTimeShiftBufferDepth(Duration value) {
        this.timeShiftBufferDepth = value;
    }


    public String getIndexRange() {
        return indexRange;
    }


    public void setIndexRange(String value) {
        this.indexRange = value;
    }


    public boolean isIndexRangeExact() {
        if (indexRangeExact == null) {
            return false;
        } else {
            return indexRangeExact;
        }
    }


    public void setIndexRangeExact(Boolean value) {
        this.indexRangeExact = value;
    }


    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
