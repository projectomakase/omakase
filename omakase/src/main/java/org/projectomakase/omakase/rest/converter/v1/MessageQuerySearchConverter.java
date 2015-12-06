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
package org.projectomakase.omakase.rest.converter.v1;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageSearchBuilder;
import org.projectomakase.omakase.job.message.MessageType;
import org.projectomakase.omakase.rest.converter.QuerySearchConverter;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

/**
 * Message {@link QuerySearchConverter} implementation.
 *
 * @author Richard Lucas
 */
public class MessageQuerySearchConverter extends QuerySearchConverter {

    private static final ImmutableBiMap<String, String> QUERY_ATTRIBUTE_MAP;
    private static final List<String> DATE_ATTRIBUTES;

    static {
        ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
        builder.put("timestamp", Message.CREATED);
        builder.put("error", Message.TYPE);
        QUERY_ATTRIBUTE_MAP = builder.build();

        DATE_ATTRIBUTES = ImmutableList.of(Message.CREATED);
    }

    @Override
    public Search from(MultivaluedMap<String, String> queryParameters) {
        Search search = super.from(queryParameters);
        List<SearchCondition> updatedSearchConditions = mapErrorConditionToTypeCondition(search);
        return createNewSearchFromSearch(search, updatedSearchConditions);
    }

    @Override
    protected Search.Builder getBuilder() {
        return new MessageSearchBuilder();
    }

    @Override
    protected ImmutableBiMap<String, String> getQueryAttributeMap() {
        return QUERY_ATTRIBUTE_MAP;
    }

    @Override
    protected boolean isDateAttribute(String attribute) {
        return DATE_ATTRIBUTES.contains(attribute);
    }

    private static Search createNewSearchFromSearch(Search search, List<SearchCondition> updatedSearchConditions) {
        MessageSearchBuilder messageSearchBuilder = new MessageSearchBuilder();
        messageSearchBuilder.conditions(updatedSearchConditions);
        messageSearchBuilder.count(search.getCount());
        messageSearchBuilder.offset(search.getOffset());
        messageSearchBuilder.onlyCount(search.isOnlyCount());
        messageSearchBuilder.orderBy(search.getOrderBy());
        messageSearchBuilder.sortOrder(search.getSortOrder());
        return messageSearchBuilder.build();
    }

    private static List<SearchCondition> mapErrorConditionToTypeCondition(Search search) {

        List<SearchCondition> isErrorConditions = search.getSearchConditions().stream().filter(searchCondition -> searchCondition.getAttribute().equals(Message.TYPE))
                .map(searchCondition -> new SearchCondition(searchCondition.getAttribute(), searchCondition.getOperator(), getMessageTypes(searchCondition.getValues()))).collect(
                        ImmutableListCollector.toImmutableList());

        List<SearchCondition> otherConditions = search.getSearchConditions().stream().filter(searchCondition -> !searchCondition.getAttribute().equals(Message.TYPE)).collect(
                ImmutableListCollector.toImmutableList());

        ImmutableList.Builder<SearchCondition> searchConditionBuilder = ImmutableList.builder();
        return searchConditionBuilder.addAll(isErrorConditions).addAll(otherConditions).build();
    }

    private static List<String> getMessageTypes(List<String> values) {
        return values.stream().map(Boolean::valueOf).map(isError -> {
            if (isError) {
                return MessageType.ERROR.name();
            } else {
                return MessageType.INFO.name();
            }
        }).collect(ImmutableListCollector.toImmutableList());
    }
}
