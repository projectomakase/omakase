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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collection;
import java.util.Map;

/**
 * Paginated Model.
 *
 * @author Richard Lucas
 */
@JsonPropertyOrder({"page", "per_page", "total_pages", "total_count", "data", "links"})
public class PaginatedEnvelope<T> {

    private Integer page;
    @JsonProperty("per_page")
    private Integer perPage;
    @JsonProperty("total_pages")
    private Long totalPages;
    @JsonProperty("total_count")
    private Long totalCount;
    private Collection<T> data;
    private Map<String, Href> links;

    public PaginatedEnvelope() {
        // required to marshal/unmarshal
    }

    public PaginatedEnvelope(Integer page, Integer perPage, Long totalPages, Long totalCount, Collection<T> data, Map<String, Href> links) {
        this.page = page;
        this.perPage = perPage;
        this.totalPages = totalPages;
        this.totalCount = totalCount;
        this.data = data;
        this.links = links;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public Long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Long totalPages) {
        this.totalPages = totalPages;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Collection<T> getData() {
        return data;
    }

    public void setData(Collection<T> data) {
        this.data = data;
    }

    public Map<String, Href> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Href> links) {
        this.links = links;
    }
}
