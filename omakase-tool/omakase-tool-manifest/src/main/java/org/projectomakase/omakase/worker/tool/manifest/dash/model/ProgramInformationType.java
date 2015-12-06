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
@XmlType(name = "ProgramInformationType", propOrder = {
        "title",
        "source",
        "copyright",
        "anies"
})
public class ProgramInformationType {

    @XmlElement(name = "Title")
    protected String title;
    @XmlElement(name = "Source")
    protected String source;
    @XmlElement(name = "Copyright")
    protected String copyright;
    @XmlAnyElement
    protected List<Element> anies;
    @XmlAttribute(name = "lang")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String lang;
    @XmlAttribute(name = "moreInformationURL")
    @XmlSchemaType(name = "anyURI")
    protected String moreInformationURL;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();


    public String getTitle() {
        return title;
    }


    public void setTitle(String value) {
        this.title = value;
    }


    public String getSource() {
        return source;
    }


    public void setSource(String value) {
        this.source = value;
    }


    public String getCopyright() {
        return copyright;
    }


    public void setCopyright(String value) {
        this.copyright = value;
    }


    public List<Element> getAnies() {
        if (anies == null) {
            anies = new ArrayList<Element>();
        }
        return this.anies;
    }


    public String getLang() {
        return lang;
    }


    public void setLang(String value) {
        this.lang = value;
    }


    public String getMoreInformationURL() {
        return moreInformationURL;
    }


    public void setMoreInformationURL(String value) {
        this.moreInformationURL = value;
    }


    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
