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
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SegmentTimelineType", propOrder = {
        "ss",
        "anies"
})
public class SegmentTimelineType {

    @XmlElement(name = "S", required = true)
    protected List<S> ss;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public List<S> getSS() {
        if (ss == null) {
            ss = new ArrayList<S>();
        }
        return this.ss;
    }


    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }


    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class S {

        @XmlAttribute(name = "t")
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger t;
        @XmlAttribute(name = "d", required = true)
        @XmlSchemaType(name = "unsignedLong")
        protected BigInteger d;
        @XmlAttribute(name = "r")
        protected BigInteger r;
        @XmlAnyAttribute
        private Map<QName, String> otherAttributes = new HashMap<QName, String>();


        public BigInteger getT() {
            return t;
        }


        public void setT(BigInteger value) {
            this.t = value;
        }


        public BigInteger getD() {
            return d;
        }


        public void setD(BigInteger value) {
            this.d = value;
        }


        public BigInteger getR() {
            if (r == null) {
                return new BigInteger("0");
            } else {
                return r;
            }
        }


        public void setR(BigInteger value) {
            this.r = value;
        }


        public Map<QName, String> getOtherAttributes() {
            return otherAttributes;
        }

    }

}
