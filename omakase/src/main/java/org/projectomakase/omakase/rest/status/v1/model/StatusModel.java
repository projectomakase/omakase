/*
 * #%L
 * omakase
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
package org.projectomakase.omakase.rest.status.v1.model;

import org.projectomakase.omakase.rest.model.v1.Href;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Rest Status Resource Representation
 *
 * @author Richard Lucas
 */
public class StatusModel {

    private String current;
    private ZonedDateTime timestamp;
    private Map<String, Href> links;

    public StatusModel() {
        // required by Jackson
    }

    public StatusModel(String current, ZonedDateTime timestamp, Map<String, Href> links) {
        this.current = current;
        this.timestamp = timestamp;
        this.links = links;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Href> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Href> links) {
        this.links = links;
    }
}
