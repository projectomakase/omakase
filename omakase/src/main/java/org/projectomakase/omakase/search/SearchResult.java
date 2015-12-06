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
package org.projectomakase.omakase.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Search Result
 *
 * @author Richard Lucas
 */
public class SearchResult<X> {

    private List<X> records = new ArrayList<>();
    private long totalRecords;

    /**
     * Constructs a new {@link SearchResult} instance.
     *
     * @param records
     *         the records returned by the search.
     * @param totalRecords
     *         the total number of records returned by the search.
     */
    public SearchResult(List<X> records, long totalRecords) {
        this.records = records;
        this.totalRecords = totalRecords;
    }

    /**
     * Returns the records returned by the search. If no records were returned by the search an empty List will be returned.
     *
     * @return the records returned by the search.
     */
    public List<X> getRecords() {
        return records;
    }

    /**
     * Returns the total number of records returned by the search.
     *
     * @return the total number of records returned by the search.
     */
    public long getTotalRecords() {
        return totalRecords;
    }
}
