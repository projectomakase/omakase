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

import java.util.Map;

/**
 * REST Response Status Representation
 *
 * @author Richard Lucas
 */
public class ResponseStatusModel {

    private ResponseStatusValue status;
    private String message;
    private String exception;
    private Map<String,Href> links;

    public ResponseStatusModel() {
    }

    public ResponseStatusModel(ResponseStatusValue status) {
        this.status = status;
    }

    public ResponseStatusModel(ResponseStatusValue status, String message) {
        this.status = status;
        this.message = message;
    }

    public ResponseStatusModel(ResponseStatusValue status, String message, String exception) {
        this.status = status;
        this.message = message;
        this.exception = exception;
    }

    public ResponseStatusValue getStatus() {
        return status;
    }

    public void setStatus(ResponseStatusValue status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public Map<String, Href> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Href> links) {
        this.links = links;
    }
}
