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
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MetricsType", propOrder = {
        "reportings",
        "ranges",
        "anies"
})
public class MetricsType {

    @XmlElement(name = "Reporting", required = true)
    protected List<DescriptorType> reportings;
    @XmlElement(name = "Range")
    protected List<RangeType> ranges;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "metrics", required = true)
    protected String metrics;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public List<DescriptorType> getReportings() {
        if (reportings == null) {
            reportings = new ArrayList<DescriptorType>();
        }
        return this.reportings;
    }


    public List<RangeType> getRanges() {
        if (ranges == null) {
            ranges = new ArrayList<RangeType>();
        }
        return this.ranges;
    }


    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }


    public String getMetrics() {
        return metrics;
    }


    public void setMetrics(String value) {
        this.metrics = value;
    }


    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
