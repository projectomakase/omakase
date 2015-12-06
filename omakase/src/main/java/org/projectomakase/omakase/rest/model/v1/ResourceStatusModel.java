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
package org.projectomakase.omakase.rest.model.v1;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.ZonedDateTime;

/**
 * REST Resource Status representation.
 *
 * @author Richard Lucas
 */
@JsonPropertyOrder({"current", "timestamp"})
public class ResourceStatusModel {

    private String current;
    private ZonedDateTime timestamp;

    public ResourceStatusModel() {
        // required by Jackson
    }

    public ResourceStatusModel(String current, ZonedDateTime timestamp) {
        this.current = current;
        this.timestamp = timestamp;
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

    public void setTimestamp(ZonedDateTime timeStamp) {
        this.timestamp = timeStamp;
    }
}
