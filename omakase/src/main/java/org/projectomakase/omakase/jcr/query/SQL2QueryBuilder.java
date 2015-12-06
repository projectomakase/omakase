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
package org.projectomakase.omakase.jcr.query;

import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SortOrder;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * JCR SQL2 query builder interface. Used to construct a JCR SQL 2 query for execution by the JCR Query Manager.
 *
 * @author Richard Lucas
 */
public interface SQL2QueryBuilder {

    /**
     * Adds search conditions to the query builder.
     *
     * @param searchConditions
     *         a list of {@link SearchCondition} instances
     * @return the builder.
     */
    SQL2QueryBuilder conditions(List<SearchCondition> searchConditions);

    /**
     * Adds a limit to the query builder.
     *
     * @param count
     *         the number of results to return
     * @param offset
     *         the result set offset
     * @return the builder.
     */
    SQL2QueryBuilder limit(@NotNull Integer count, @NotNull Integer offset);

    /**
     * Adds a order by to the builder.
     *
     * @param sortAttribute
     *         the sort attribute
     * @param sortOrder
     *         the sort order
     * @return the builder.
     */
    SQL2QueryBuilder orderBy(@NotNull String sortAttribute, @NotNull SortOrder sortOrder);

    /**
     * Builds the JCR SQL 2 query.
     *
     * @return the JCR SQL 2 query.
     */
    String build();
}
