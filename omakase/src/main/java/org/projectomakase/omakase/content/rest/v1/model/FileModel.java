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
package org.projectomakase.omakase.content.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.projectomakase.omakase.rest.model.v1.Href;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST File Representation
 *
 * @author Richard Lucas
 */
@JsonPropertyOrder({"id", "name", "size", "original_filename", "hashes", "created", "links"})
public class FileModel {

    private String id;
    private String name;
    private Long size;
    @JsonProperty("original_filename")
    private String originalFileName;
    private List<HashModel> hashes;
    private ZonedDateTime created;
    private Map<String,Href> links;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public List<HashModel> getHashes() {
        return hashes;
    }

    public void setHashes(List<HashModel> hashes) {
        this.hashes = hashes;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public Map<String, Href> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Href> links) {
        this.links = links;
    }
}
