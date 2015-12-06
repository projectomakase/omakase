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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class SearchBuilderTest {

    private Search.Builder searchBuilder;

    @Before
    public void before() {
        searchBuilder = new Search.Builder() {
            @Override
            protected List<String> getSupportedSortAttributes() {
                return ImmutableList.of("test");
            }

            @Override
            protected Multimap<String, Operator> getSupportedConditions() {
                return ImmutableMultimap.of("test", Operator.EQ);
            }
        };
    }

    @Test
    public void shouldBuildSearch() {
        SearchCondition searchCondition = new SearchCondition("test", Operator.EQ, ImmutableList.of("value"));
        Search search = searchBuilder.orderBy("test").sortOrder(SortOrder.DESC).conditions(ImmutableList.of(searchCondition)).build();
        assertThat(search).isEqualToComparingFieldByField(new Search(ImmutableList.of(searchCondition), "test", SortOrder.DESC, 0, 10, false));
    }

    @Test
    public void shouldFailToBuildSearchInvalidSortAttribute() {
        try {
            searchBuilder.orderBy("bad").sortOrder(SortOrder.DESC).conditions(ImmutableList.of()).build();
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid sort attribute bad");
        }
    }

    @Test
    public void shouldFailToBuildSearchInvalidConditionAttribute() {
        try {
            SearchCondition searchCondition = new SearchCondition("bad", Operator.EQ, ImmutableList.of("value"));
            searchBuilder.orderBy("test").sortOrder(SortOrder.DESC).conditions(ImmutableList.of(searchCondition)).build();
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid search condition attribute bad");
        }
    }

    @Test
    public void shouldFailToBuildSearchInvalidConditionOperator() {
        try {
            SearchCondition searchCondition = new SearchCondition("test", Operator.NE, ImmutableList.of("value"));
            searchBuilder.orderBy("test").sortOrder(SortOrder.DESC).conditions(ImmutableList.of(searchCondition)).build();
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid search condition operator NE for condition attribute test");
        }
    }

}