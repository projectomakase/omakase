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
package org.projectomakase.omakase.rest.pagination.v1;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Map;

/**
 * Pagination Links.
 *
 * @author Richard Lucas
 */
public class PaginationLinks {

    private final Map<String, Href> links;
    private final long totalPages;

    /**
     * Creates Pagination Links for the given Uri.
     *
     * @param uriInfo
     *         uriInfo representing the current request URI.
     * @param page
     *         the current page.
     * @param pageSize
     *         the page size.
     * @param totalCount
     *         the total count of items being paginated.
     */
    public PaginationLinks(UriInfo uriInfo, int page, int pageSize, long totalCount) {
        ImmutableMap.Builder<String, Href> linksBuilder = ImmutableMap.builder();
        linksBuilder.put("first", new Href(getLinkUri(uriInfo, 1, pageSize).toString()));

        long pages = 1;
        if (totalCount > pageSize) {
            pages = LongMath.divide(totalCount, pageSize, RoundingMode.CEILING);
        }
        if (pages > 1) {
            linksBuilder.put("last", new Href(getLinkUri(uriInfo, pages, pageSize).toString()));
        }

        if (page > 1) {
            int prev = page - 1;
            if (prev < pages) {
                linksBuilder.put("prev", new Href(getLinkUri(uriInfo, prev, pageSize).toString()));
            }
        }

        int next = page + 1;
        if (next <= pages) {
            linksBuilder.put("next", new Href(getLinkUri(uriInfo, next, pageSize).toString()));
        }
        this.links = linksBuilder.build();
        this.totalPages = pages;
    }

    /**
     * Returns the available pagination links.
     *
     * @return the available pagination links.
     */
    public Map<String, Href> get() {
        return links;
    }

    /**
     * Returns the total number of totalPages
     *
     * @return the total number of totalPages
     */
    public long getTotalPages() {
        return totalPages;
    }

    private static URI getLinkUri(UriInfo uriInfo, long page, int pageSize) {
        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        uriBuilder.queryParam("page", page);
        uriBuilder.queryParam("per_page", pageSize);
        return uriBuilder.build();
    }
}
