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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.projectomakase.omakase.search.DefaultSearchBuilder;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SortOrder;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSQL2QueryBuilderTest {

    private DefaultSQL2QueryBuilder defaultSQL2QueryBuilder;
    private Search.Builder testBuilder;

    @Before
    public void before() {
        defaultSQL2QueryBuilder = new DefaultSQL2QueryBuilder("test", "/test");
        testBuilder = new Search.Builder() {
            @Override
            protected List<String> getSupportedSortAttributes() {
                return ImmutableList.of("test:attr1");
            }

            @Override
            protected Multimap<String, Operator> getSupportedConditions() {
                ImmutableMultimap.Builder<String, Operator> builder = ImmutableMultimap.builder();
                builder.put("test:attr1", Operator.EQ);
                builder.putAll("test:attr2", Operator.EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE, Operator.NE);
                return builder.build();
            }
        };
    }

    @Test
    public void shouldBuildQueryUsingDefaultSearch() {
        Search search = new DefaultSearchBuilder().build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        defaultSQL2QueryBuilder.orderBy(search.getOrderBy(), search.getSortOrder());
        defaultSQL2QueryBuilder.limit(search.getCount(), search.getOffset());
        assertThat(defaultSQL2QueryBuilder.build()).isEqualTo("SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test') LIMIT 10 OFFSET 0");
    }

    @Test
    public void shouldBuildQueryUsingDefaultSearchNoLimit() {
        Search search = new DefaultSearchBuilder().build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        defaultSQL2QueryBuilder.orderBy(search.getOrderBy(), search.getSortOrder());
        assertThat(defaultSQL2QueryBuilder.build()).isEqualTo("SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test')");
    }

    @Test
    public void shouldBuildQueryWithOrderBy() {
        Search search = testBuilder.orderBy("test:attr1").build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        defaultSQL2QueryBuilder.orderBy(search.getOrderBy(), search.getSortOrder());
        defaultSQL2QueryBuilder.limit(search.getCount(), search.getOffset());
        assertThat(defaultSQL2QueryBuilder.build()).isEqualTo("SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test') ORDER by node.[test:attr1] ASC LIMIT 10 OFFSET 0");
    }

    @Test
    public void shouldBuildQueryWithSingleCondition() {
        Search search = testBuilder.conditions(ImmutableList.of(new SearchCondition("test:attr1", Operator.EQ, "value1"))).build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        assertThat(defaultSQL2QueryBuilder.build()).isEqualTo("SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test') AND (node.[test:attr1] = 'value1')");
    }

    @Test
    public void shouldBuildQueryWithMultipleAndConditions() {
        Search search = testBuilder.conditions(ImmutableList
                .of(new SearchCondition("test:attr1", Operator.EQ, "value1"), new SearchCondition("test:attr2", Operator.NE, "value1"), new SearchCondition("test:attr2", Operator.GTE, "value2")))
                .build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        assertThat(defaultSQL2QueryBuilder.build()).isEqualTo(
                "SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test') AND (node.[test:attr1] = 'value1') AND (node.[test:attr2] != 'value1') AND (node.[test:attr2] >= 'value2')");
    }

    @Test
    public void shouldBuildQueryWithOrConditions() {
        Search search = testBuilder.conditions(ImmutableList.of(new SearchCondition("test:attr1", Operator.EQ, "value1,value2,value3"))).build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        assertThat(defaultSQL2QueryBuilder.build())
                .isEqualTo("SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test') AND (node.[test:attr1] = 'value1' OR node.[test:attr1] = 'value2' OR node.[test:attr1] = 'value3')");
    }

    @Test
    public void shouldBuildComplexQuery() {
        Search search = testBuilder.conditions(ImmutableList.of(new SearchCondition("test:attr1", Operator.EQ, "value1,value2,value3"), new SearchCondition("test:attr2", Operator.NE, "value1"),
                new SearchCondition("test:attr2", Operator.LT, "value2"))).orderBy("test:attr1").sortOrder(SortOrder.DESC).build();
        defaultSQL2QueryBuilder.conditions(search.getSearchConditions());
        defaultSQL2QueryBuilder.orderBy(search.getOrderBy(), search.getSortOrder());
        defaultSQL2QueryBuilder.limit(search.getCount(), search.getOffset());
        assertThat(defaultSQL2QueryBuilder.build()).isEqualTo(
                "SELECT node.* FROM [test] AS node WHERE ISDESCENDANTNODE('/test') AND (node.[test:attr1] = 'value1' OR node.[test:attr1] = 'value2' OR node.[test:attr1] = 'value3') AND (node.[test:attr2] != 'value1') AND (node.[test:attr2] < 'value2') ORDER by node.[test:attr1] DESC LIMIT 10 OFFSET 0");
    }
}