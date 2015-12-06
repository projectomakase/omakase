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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SearchException;
import org.projectomakase.omakase.search.SortOrder;
import org.projectomakase.omakase.time.DateTimeCalculator;
import org.projectomakase.omakase.time.DateTimeStartEnd;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.validation.constraints.NotNull;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * @author Richard Lucas
 */
public abstract class AbstractSQL2QueryBuilder implements SQL2QueryBuilder {

    private static final Logger LOGGER = Logger.getLogger(AbstractSQL2QueryBuilder.class);

    protected List<SearchCondition> searchConditions = ImmutableList.of();
    protected Integer count;
    protected Integer offset;
    protected String sortAttribute;
    protected SortOrder sortOrder;

    @Override
    public AbstractSQL2QueryBuilder conditions(List<SearchCondition> searchConditions) {
        this.searchConditions = searchConditions;
        return this;
    }

    @Override
    public AbstractSQL2QueryBuilder limit(@NotNull Integer count, @NotNull Integer offset) {
        this.count = count;
        this.offset = offset;
        return this;
    }

    @Override
    public AbstractSQL2QueryBuilder orderBy(@NotNull String sortAttribute, @NotNull SortOrder sortOrder) {
        this.sortAttribute = sortAttribute;
        this.sortOrder = sortOrder;
        return this;
    }

    protected String build(String selectClause) {
        StringBuilder stringBuilder = new StringBuilder(selectClause);
        getJcrSql2Conditions(searchConditions).forEach(stringBuilder::append);
        getOrderBy(sortAttribute, sortOrder).ifPresent(stringBuilder::append);
        getLimit(count, offset).ifPresent(stringBuilder::append);

        String sql = stringBuilder.toString();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(sql);
        }

        return sql;
    }

    static List<String> getJcrSql2Conditions(List<SearchCondition> searchConditions) {
        return searchConditions.stream().map(AbstractSQL2QueryBuilder::getJcrSql2ConditionsForValue).collect(ImmutableListsCollector.toImmutableList());
    }

    static Optional<String> getOrderBy(String sortAttribute, SortOrder sortOrder) {
        if (sortAttribute != null && sortOrder != null) {
            return Optional.of(String.format(" ORDER by node.[%s] %s", sortAttribute, sortOrder.name()));
        } else {
            return Optional.empty();
        }
    }

    static Optional<String> getLimit(Integer count, Integer offset)  {
        if (count != null && offset != null) {
            return Optional.of(String.format(" LIMIT %d OFFSET %d", count, offset));
        } else {
            return  Optional.empty();
        }
    }

    static List<String> getJcrSql2ConditionsForValue(SearchCondition searchCondition) {
        return searchCondition.getValues().stream().map(value -> getJcrSql2Condition(searchCondition.getAttribute(), searchCondition.getOperator(), value, searchCondition.isDate())).collect(
                ImmutableListCollector.toImmutableList());
    }

    static String getJcrSql2Condition(String attribute, Operator operator, String value, boolean isDate) {
        Iterable<String> iterable = Splitter.on(",").trimResults().split(value);
        StringBuilder stringBuilder = new StringBuilder(" AND (");
        String jcrComparisonConstraint = getJcrSQL2ComparisonConstraint(operator);
        iterable.forEach(v -> {
            if (isDate) {
                stringBuilder.append(getDateCondition(attribute, operator, v));
            } else {
                if ("LIKE".equals(jcrComparisonConstraint)) {
                    v = "%" + v + "%";
                }
                stringBuilder.append(String.format("node.[%s] %s '%s' OR ", attribute, jcrComparisonConstraint, v));
            }
        });
        stringBuilder.append(")");
        return stringBuilder.toString().replace(" OR )", ")");
    }

    static String getDateCondition(String attribute, Operator operator, String value) {

        DateTimeStartEnd between = DateTimeCalculator.calculateStartAndEndDates(value);
        String start = between.getStart().format(DateTimeFormatter.ISO_DATE_TIME);
        String end = between.getEnd().format(DateTimeFormatter.ISO_DATE_TIME);

        String condition;
        switch (operator) {
            case EQ:
                condition = String.format("node.[%s] BETWEEN '%s' AND '%s' OR ", attribute, start, end);
                break;
            case GT:
                condition = String.format("node.[%s] > '%s' OR ", attribute, end);
                break;
            case GTE:
                condition = String.format("node.[%s] >= '%s' OR ", attribute, start);
                break;
            case LT:
                condition = String.format("node.[%s] < '%s' OR ", attribute, start);
                break;
            case LTE:
                condition = String.format("node.[%s] <= '%s' OR ", attribute, end);
                break;
            default:
                throw new SearchException("Error generating Jcr SQL2 query. Unsupported date operator " + operator);
        }
        return condition;
    }

     static String getJcrSQL2ComparisonConstraint(Operator operator) {
        String constraint;
        switch (operator) {
            case EQ:
                constraint = "=";
                break;
            case NE:
                constraint = "!=";
                break;
            case LIKE:
                constraint = "LIKE";
                break;
            case GT:
                constraint = ">";
                break;
            case GTE:
                constraint = ">=";
                break;
            case LT:
                constraint = "<";
                break;
            case LTE:
                constraint = "<=";
                break;
            default:
                throw new SearchException("Error generating Jcr SQL2 query. Unknown operator " + operator);
        }
        return constraint;
    }
}
