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

import com.google.common.collect.Multimap;
import org.jboss.logging.Logger;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the search conditions, sorting and pagination.
 * <p>
 * Search objects should not be created directly but instead should be created using a {@link Search.Builder} implementation that is responsible for validating the
 * search for the given search domain.
 * </p>
 *
 * @author Richard Lucas
 */
public class Search {

    private final List<SearchCondition> searchConditions;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final int offset;
    private final int count;
    private final boolean onlyCount;

    /**
     * Constructor used to create a new search. This constructor should NEVER be called directly. Instead search objects should be built using a {@link Search.Builder}
     * implementation for the current search domain.
     *
     * @param searchConditions
     *         a list of {@link SearchCondition}s
     * @param orderBy
     *         the order by attribute
     * @param sortOrder
     *         the sort order (ASC, DESC)
     * @param offset
     *         the search result offset (zero based)
     * @param count
     *         the maximum number of results to return
     * @param onlyCount
     *         true if only the record count should be returned otherwise false. If true the offset and count are ignored.
     */
    protected Search(List<SearchCondition> searchConditions, String orderBy, SortOrder sortOrder, int offset, int count, boolean onlyCount) {
        this.searchConditions = searchConditions;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.offset = offset;
        this.count = count;
        this.onlyCount = onlyCount;
    }

    /**
     * Returns a list  of {@link SearchCondition}s.
     *
     * @return a list  of {@link SearchCondition}s.
     */
    public List<SearchCondition> getSearchConditions() {
        return searchConditions;
    }

    /**
     * Returns the order by attribute.
     *
     * @return the order by attribute.
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * Returns the sort order (ASC, DESC).
     *
     * @return the sort order (ASC, DESC).
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * Returns the search result offset (zero based).
     *
     * @return the search result offset (zero based).
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Return the maximum number of results to return.
     *
     * @return the maximum number of results to return.
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns true if only the record count should be returned otherwise false. If true the offset and count are ignored.
     *
     * @return true if only the record count should be returned otherwise false. If true the offset and count are ignored.
     */
    public boolean isOnlyCount() {
        return onlyCount;
    }

    /**
     * Abstract Search Builder. Implementations of this are used to create and validate search objects for specific search domains.
     */
    public abstract static class Builder {

        private static final Logger LOGGER = Logger.getLogger(Builder.class);

        private List<SearchCondition> searchConditions = new ArrayList<>();
        private String orderBy = getDefaultOrderBy();
        private SortOrder sortOrder = getDefaultSortOrder();
        private int offset = 0;
        private int count = 10;
        private boolean onlyCount;


        public Builder condition(@NotNull SearchCondition searchCondition) {
            this.searchConditions.add(searchCondition);
            return this;
        }

        public Builder conditions(@NotNull List<SearchCondition> searchConditions) {
            this.searchConditions.addAll(searchConditions);
            return this;
        }

        public Builder orderBy(@NotNull String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder sortOrder(@NotNull SortOrder sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder onlyCount(boolean onlyCount) {
            this.onlyCount = onlyCount;
            return this;
        }

        public Search build() {
            if (orderBy != null && !getSupportedSortAttributes().contains(orderBy)) {
                throw new InvalidSearchConditionException("Invalid sort attribute", orderBy);
            }
            searchConditions.forEach(this::validateCondition);
            return new Search(searchConditions, orderBy, sortOrder, offset, count, onlyCount);
        }

        private void validateCondition(SearchCondition searchCondition) {
            String attribute = searchCondition.getAttribute();
            Operator operator = searchCondition.getOperator();

            if (!getSupportedConditions().containsKey(attribute)) {
                LOGGER.error("Invalid search condition attribute " + attribute);
                throw new InvalidSearchConditionException("Invalid search condition attribute", attribute);
            }

            if (!getSupportedConditions().get(attribute).contains(operator)) {
                LOGGER.error("Invalid search condition operator " + operator + " for condition attribute " + attribute);
                throw new InvalidSearchConditionException("Invalid search condition operator " + operator + " for condition attribute", attribute);
            }
        }

        /**
         * Returns the default order by used by the builder.
         *
         * @return the default order by used by the builder.
         */
        protected String getDefaultOrderBy() {
            return null;
        }

        /**
         * Returns the default sort order used by the builder.
         *
         * @return the default sort order used by the builder.
         */
        protected SortOrder getDefaultSortOrder() {
            return SortOrder.ASC;
        }

        /**
         * Returns a list of sort attributes supported in the current search domain.
         *
         * @return a list of sort attributes supported in the current search domain.
         */
        protected abstract List<String> getSupportedSortAttributes();

        /**
         * Returns a multi map of conditions supported in the current search domain.
         *
         * @return a multi map of conditions supported in the current search domain.
         */
        protected abstract Multimap<String, Operator> getSupportedConditions();

    }
}
