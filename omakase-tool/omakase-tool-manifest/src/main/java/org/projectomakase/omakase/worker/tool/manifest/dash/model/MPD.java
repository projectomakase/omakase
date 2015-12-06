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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MPDtype", propOrder = {
        "programInformations",
        "baseURLs",
        "locations",
        "periods",
        "metrics",
        "anies"
})
@XmlRootElement(name = "MPD", namespace = "urn:mpeg:dash:schema:mpd:2011")
public class MPD {

    @XmlElement(name = "ProgramInformation")
    protected List<ProgramInformationType> programInformations;
    @XmlElement(name = "BaseURL")
    protected List<BaseURLType> baseURLs;
    @XmlElement(name = "Location")
    @XmlSchemaType(name = "anyURI")
    protected List<String> locations;
    @XmlElement(name = "Period", required = true)
    protected List<PeriodType> periods;
    @XmlElement(name = "Metrics")
    protected List<MetricsType> metrics;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "profiles", required = true)
    protected String profiles;
    @XmlAttribute(name = "type")
    protected PresentationType type;
    @XmlAttribute(name = "availabilityStartTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar availabilityStartTime;
    @XmlAttribute(name = "availabilityEndTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar availabilityEndTime;
    @XmlAttribute(name = "mediaPresentationDuration")
    protected Duration mediaPresentationDuration;
    @XmlAttribute(name = "minimumUpdatePeriod")
    protected Duration minimumUpdatePeriod;
    @XmlAttribute(name = "minBufferTime", required = true)
    protected Duration minBufferTime;
    @XmlAttribute(name = "timeShiftBufferDepth")
    protected Duration timeShiftBufferDepth;
    @XmlAttribute(name = "suggestedPresentationDelay")
    protected Duration suggestedPresentationDelay;
    @XmlAttribute(name = "maxSegmentDuration")
    protected Duration maxSegmentDuration;
    @XmlAttribute(name = "maxSubsegmentDuration")
    protected Duration maxSubsegmentDuration;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public List<ProgramInformationType> getProgramInformations() {
        if (programInformations == null) {
            programInformations = new ArrayList<ProgramInformationType>();
        }
        return this.programInformations;
    }


    public List<BaseURLType> getBaseURLs() {
        if (baseURLs == null) {
            baseURLs = new ArrayList<BaseURLType>();
        }
        return this.baseURLs;
    }


    public List<String> getLocations() {
        if (locations == null) {
            locations = new ArrayList<String>();
        }
        return this.locations;
    }


    public List<PeriodType> getPeriods() {
        if (periods == null) {
            periods = new ArrayList<PeriodType>();
        }
        return this.periods;
    }


    public List<MetricsType> getMetrics() {
        if (metrics == null) {
            metrics = new ArrayList<MetricsType>();
        }
        return this.metrics;
    }


    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }


    public String getId() {
        return id;
    }


    public void setId(String value) {
        this.id = value;
    }


    public String getProfiles() {
        return profiles;
    }


    public void setProfiles(String value) {
        this.profiles = value;
    }


    public PresentationType getType() {
        if (type == null) {
            return PresentationType.STATIC;
        } else {
            return type;
        }
    }


    public void setType(PresentationType value) {
        this.type = value;
    }


    public XMLGregorianCalendar getAvailabilityStartTime() {
        return availabilityStartTime;
    }


    public void setAvailabilityStartTime(XMLGregorianCalendar value) {
        this.availabilityStartTime = value;
    }


    public XMLGregorianCalendar getAvailabilityEndTime() {
        return availabilityEndTime;
    }


    public void setAvailabilityEndTime(XMLGregorianCalendar value) {
        this.availabilityEndTime = value;
    }


    public Duration getMediaPresentationDuration() {
        return mediaPresentationDuration;
    }


    public void setMediaPresentationDuration(Duration value) {
        this.mediaPresentationDuration = value;
    }


    public Duration getMinimumUpdatePeriod() {
        return minimumUpdatePeriod;
    }


    public void setMinimumUpdatePeriod(Duration value) {
        this.minimumUpdatePeriod = value;
    }


    public Duration getMinBufferTime() {
        return minBufferTime;
    }


    public void setMinBufferTime(Duration value) {
        this.minBufferTime = value;
    }


    public Duration getTimeShiftBufferDepth() {
        return timeShiftBufferDepth;
    }


    public void setTimeShiftBufferDepth(Duration value) {
        this.timeShiftBufferDepth = value;
    }


    public Duration getSuggestedPresentationDelay() {
        return suggestedPresentationDelay;
    }


    public void setSuggestedPresentationDelay(Duration value) {
        this.suggestedPresentationDelay = value;
    }


    public Duration getMaxSegmentDuration() {
        return maxSegmentDuration;
    }


    public void setMaxSegmentDuration(Duration value) {
        this.maxSegmentDuration = value;
    }


    public Duration getMaxSubsegmentDuration() {
        return maxSubsegmentDuration;
    }


    public void setMaxSubsegmentDuration(Duration value) {
        this.maxSubsegmentDuration = value;
    }


    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
