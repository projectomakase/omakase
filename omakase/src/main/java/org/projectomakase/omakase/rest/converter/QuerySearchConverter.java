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
package org.projectomakase.omakase.rest.converter;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.search.InvalidSearchConditionException;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SortOrder;
import org.jboss.logging.Logger;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts filter, sort and pagination query parameters into a {@link Search}. Implementations are required to provide a search builder and a mapping between query
 * attribute names and search attribute names.
 *
 * @author Richard Lucas
 */
public abstract class QuerySearchConverter {

    private static final Logger LOGGER = Logger.getLogger(QuerySearchConverter.class);

    private static final List<String> SEARCH_QUERY_PARAMETERS = ImmutableList.of("page", "per_page", "sort", "order", "only_count");
    private static final Pattern PATTERN = Pattern.compile("((?:[^\\[]+))\\[((?:[a-z]+))\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Returns the {@link Search.Builder} implementation used to build the search.
     *
     * @return the {@link Search.Builder} implementation used to build the search.
     */
    protected abstract Search.Builder getBuilder();

    /**
     * Returns an ImmutableBiMap containing mappings between the filter and sort query attributes and the corresponding {@link Search} implementations search domain
     * attributes.
     *
     * @return an ImmutableBiMap containing mappings between the filter and sort query attributes and the corresponding {@link Search} implementations search domain
     * attributes.
     */
    protected abstract ImmutableBiMap<String, String> getQueryAttributeMap();

    /**
     * Returns true if the attribute is a date, otherwise false.
     *
     * @param attribute
     *         the attribute
     * @return true if the attribute is a date, otherwise false.
     */
    protected abstract boolean isDateAttribute(String attribute);

    /**
     * Creates a new {@link Search} from te specified query parameters.
     *
     * @param queryParameters
     *         query parameters
     * @return a new {@link Search} from te specified query parameters.
     */
    public Search from(MultivaluedMap<String, String> queryParameters) {

        int page = Optional.ofNullable(queryParameters.getFirst("page")).map(Integer::valueOf).orElse(1);
        int perPage = Optional.ofNullable(queryParameters.getFirst("per_page")).map(Integer::valueOf).orElse(10);
        int offset = (page * perPage) - perPage;

        final Search.Builder searchBuilder = getBuilder();
        searchBuilder.count(perPage);
        searchBuilder.offset(offset);
        Optional.ofNullable(queryParameters.getFirst("sort")).ifPresent(sort -> searchBuilder.orderBy(getOrderBy(sort)));
        Optional.ofNullable(queryParameters.getFirst("order")).map(QuerySearchConverter::getSortOder).ifPresent(searchBuilder::sortOrder);
        Optional.ofNullable(queryParameters.getFirst("only_count")).map(Boolean::valueOf).ifPresent(searchBuilder::onlyCount);
        ImmutableList.Builder<SearchCondition> listBuilder = ImmutableList.builder();
        queryParameters.forEach((key, value) -> {
            if (!SEARCH_QUERY_PARAMETERS.contains(key)) {
                listBuilder.add(getCondition(key, value));
            }
        });
        searchBuilder.conditions(listBuilder.build());

        Search search;
        try {
            search = searchBuilder.build();
        } catch (InvalidSearchConditionException e) {
            String queryAttribute = getQueryAttributeMap().inverse().get(e.getInvalidAttribute());
            throw new InvalidSearchConditionException(e.getOriginalMessage(), queryAttribute, e);
        }

        return search;
    }

    private SearchCondition getCondition(@NotNull String key, @NotNull List<String> value) {
        Matcher matcher = PATTERN.matcher(key);
        if (matcher.find()) {
            String queryAttribute = matcher.group(1);
            String operator = matcher.group(2);
            if (!getQueryAttributeMap().containsKey(queryAttribute)) {
                LOGGER.error("Invalid search condition attribute " + queryAttribute);
                throw new InvalidSearchConditionException("Invalid search condition attribute", queryAttribute);
            }
            String attribute = getQueryAttributeMap().get(queryAttribute);
            boolean isDate = isDateAttribute(attribute);
            if (isDate) {
                validateDates(queryAttribute, value);
            }
            return new SearchCondition(attribute, getOperator(operator), value, isDate);
        } else {
            LOGGER.error("Invalid query parameter " + key);
            throw new InvalidSearchConditionException("Invalid query parameter", key);
        }
    }

    private String getOrderBy(@NotNull String sort) {
        if (!getQueryAttributeMap().containsKey(sort)) {
            LOGGER.error("Invalid sort attribute " + sort);
            throw new InvalidSearchConditionException("Invalid sort attribute", sort);
        }
        return getQueryAttributeMap().get(sort);
    }

    private static SortOrder getSortOder(@NotNull String order) {
        try {
            return SortOrder.valueOf(order.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidSearchConditionException("Invalid sort order", order, e);
        }
    }

    private static Operator getOperator(@NotNull String operator) {
        try {
            return Operator.valueOf(operator.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidSearchConditionException("Invalid operator", operator, e);
        }
    }

    private static void validateDates(String queryAttribute, List<String> values) {
        values.forEach(value -> {
            Iterable<String> iterable = Splitter.on(",").trimResults().split(value);
            iterable.forEach(splitValue -> {
                if (splitValue.contains("T")) {
                    parseDate(queryAttribute, splitValue, DateTimeFormatter.ISO_DATE_TIME);
                } else {
                    parseDate(queryAttribute, splitValue, DateTimeFormatter.ISO_DATE);
                }
            });
        });

    }

    private static void parseDate(String queryAttribute, String value, DateTimeFormatter dateTimeFormatter) {
        try {
            dateTimeFormatter.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidSearchConditionException("Invalid date format for attribute", queryAttribute, e);
        }
    }
}
