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
package org.projectomakase.omakase.broker.rest.v1.converter;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.broker.WorkerSearchBuilder;
import org.projectomakase.omakase.rest.converter.QuerySearchConverter;
import org.projectomakase.omakase.search.Search;

import java.util.List;

/**
 * Worker implementation of the {@link QuerySearchConverter}
 *
 * @author Richard Lucas
 */
public class WorkerQuerySearchConverter extends QuerySearchConverter {

    private static final ImmutableBiMap<String, String> QUERY_ATTRIBUTE_MAP;
    private static final List<String> DATE_ATTRIBUTES;

    static {
        ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
        builder.put("id", Worker.ID);
        builder.put("name", Worker.WORKER_NAME);
        builder.put("external_ids", Worker.EXTERNAL_IDS);
        builder.put("status.current", Worker.STATUS);
        builder.put("status.timestamp", Worker.STATUS_TIMESTAMP);
        builder.put("created", Worker.CREATED);
        builder.put("created_by", Worker.CREATED_BY);
        builder.put("last_modified", Worker.LAST_MODIFIED);
        builder.put("last_modified_by", Worker.LAST_MODIFIED_BY);
        QUERY_ATTRIBUTE_MAP = builder.build();

        DATE_ATTRIBUTES = ImmutableList.of(Worker.STATUS_TIMESTAMP, Worker.CREATED, Worker.LAST_MODIFIED_BY);
    }

    @Override
    protected Search.Builder getBuilder() {
        return new WorkerSearchBuilder();
    }

    @Override
    protected ImmutableBiMap<String, String> getQueryAttributeMap() {
        return QUERY_ATTRIBUTE_MAP;
    }

    @Override
    protected boolean isDateAttribute(String attribute) {
        return DATE_ATTRIBUTES.contains(attribute);
    }
}
