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
package org.projectomakase.omakase.content.rest.v1.converter;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantRepositorySearchBuilder;
import org.projectomakase.omakase.rest.converter.QuerySearchConverter;
import org.projectomakase.omakase.search.Search;

import java.util.List;

/**
 * Asset implementation of the {@link QuerySearchConverter}
 *
 * @author Richard Lucas
 */
public class RepositoryQuerySearchConverter extends QuerySearchConverter {

    private static final ImmutableBiMap<String, String> QUERY_ATTRIBUTE_MAP;
    private static final List<String> DATE_ATTRIBUTES;

    static {
        ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
        builder.put("id", VariantRepository.ID);
        builder.put("name", VariantRepository.REPOSITORY_NAME);
        builder.put("type", VariantRepository.TYPE);
        builder.put("created", VariantRepository.CREATED);
        builder.put("created_by", VariantRepository.CREATED_BY);
        builder.put("last_modified", VariantRepository.LAST_MODIFIED);
        builder.put("last_modified_by", VariantRepository.LAST_MODIFIED_BY);
        QUERY_ATTRIBUTE_MAP = builder.build();

        DATE_ATTRIBUTES = ImmutableList.of(VariantRepository.CREATED, VariantRepository.LAST_MODIFIED_BY);
    }

    @Override
    protected Search.Builder getBuilder() {
        return new VariantRepositorySearchBuilder();
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
