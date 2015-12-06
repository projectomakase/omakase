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
package org.projectomakase.omakase.content;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SortOrder;

import java.util.List;

/**
 * Variant specific implementation of {@link Search.Builder}.
 *
 * @author Richard Lucas
 */
public class VariantSearchBuilder extends Search.Builder {

    private static final List<String> SUPPORTED_SORT_ATTRIBUTES;
    private static final Multimap<String, Operator> SUPPORTED_CONDITIONS;

    static {
        SUPPORTED_SORT_ATTRIBUTES = ImmutableList.of(Variant.ID, Variant.VARIANT_NAME, Variant.CREATED, Variant.CREATED_BY, Variant.LAST_MODIFIED, Variant.LAST_MODIFIED_BY);

        ImmutableMultimap.Builder<String, Operator> mapBuilder = ImmutableMultimap.builder();
        mapBuilder.put(Variant.ID, Operator.EQ);
        mapBuilder.putAll(Variant.VARIANT_NAME, Operator.EQ, Operator.LIKE);
        mapBuilder.put(Variant.EXTERNAL_IDS, Operator.EQ);
        mapBuilder.putAll(Variant.CREATED, Operator.EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE);
        mapBuilder.putAll(Variant.CREATED_BY, Operator.EQ, Operator.NE);
        mapBuilder.putAll(Variant.LAST_MODIFIED, Operator.EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE);
        mapBuilder.putAll(Variant.LAST_MODIFIED_BY, Operator.EQ, Operator.NE);

        SUPPORTED_CONDITIONS = mapBuilder.build();
    }

    @Override
    protected List<String> getSupportedSortAttributes() {
        return SUPPORTED_SORT_ATTRIBUTES;
    }

    @Override
    protected Multimap<String, Operator> getSupportedConditions() {
        return SUPPORTED_CONDITIONS;
    }

    @Override
    protected String getDefaultOrderBy() {
        return Variant.CREATED;
    }

    @Override
    protected SortOrder getDefaultSortOrder() {
        return SortOrder.DESC;
    }
}
