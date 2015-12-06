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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.projectomakase.omakase.search.InvalidSearchConditionException;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SortOrder;
import org.assertj.core.groups.Tuple;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class QuerySearchConverterTest {

    private QuerySearchConverter converter;
    private MultivaluedMap<String, String> multivaluedMap;

    @Before
    public void before() {
        converter = new QuerySearchConverter() {
            @Override
            protected Search.Builder getBuilder() {
                return new Search.Builder() {
                    @Override
                    protected List<String> getSupportedSortAttributes() {
                        return ImmutableList.of("test:test", "test:test1_abc");
                    }

                    @Override
                    protected Multimap<String, Operator> getSupportedConditions() {
                        return ImmutableMultimap.of("test:test", Operator.EQ, "test:test1_abc", Operator.EQ);
                    }
                };
            }

            @Override
            protected ImmutableBiMap<String, String> getQueryAttributeMap() {
                return ImmutableBiMap.of("test", "test:test", "test1_abc", "test:test1_abc");
            }

            @Override
            protected boolean isDateAttribute(String attribute) {
                return false;
            }
        };
        multivaluedMap = new MultivaluedMapImpl<>();
    }

    @Test
    public void shouldConvertPerPage() {
        multivaluedMap.addFirst("per_page", "20");
        assertThat(converter.from(multivaluedMap).getCount()).isEqualTo(20);
    }

    @Test
    public void shouldConvertPage() {
        multivaluedMap.addFirst("page", "5");
        multivaluedMap.addFirst("per_page", "20");
        assertThat(converter.from(multivaluedMap).getOffset()).isEqualTo(80);
    }

    @Test
    public void shouldConvertPageNoPerPage() {
        multivaluedMap.addFirst("page", "5");
        assertThat(converter.from(multivaluedMap).getOffset()).isEqualTo(40);
    }

    @Test
    public void shouldConvertSortAttribute() {
        multivaluedMap.addFirst("sort", "test");
        assertThat(converter.from(multivaluedMap).getOrderBy()).isEqualTo("test:test");
    }

    @Test
    public void shouldThrowInvalidSearchConditionBadSortAttribute() {
        multivaluedMap.addFirst("sort", "bad");
        try {
            converter.from(multivaluedMap);
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid sort attribute bad");
        }
    }

    @Test
    public void shouldConvertOrder() {
        multivaluedMap.addFirst("order", "DESC");
        assertThat(converter.from(multivaluedMap).getSortOrder()).isEqualTo(SortOrder.DESC);
    }

    @Test
    public void shouldThrowInvalidSearchConditionBadSortOrder() {
        multivaluedMap.addFirst("order", "bad");
        try {
            converter.from(multivaluedMap);
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid sort order bad");
        }
    }

    @Test
    public void shouldConvertOnlyCountTrue() {
        multivaluedMap.addFirst("only_count", "true");
        assertThat(converter.from(multivaluedMap).isOnlyCount()).isTrue();
    }

    @Test
    public void shouldConvertOnlyCountFalse() {
        multivaluedMap.addFirst("only_count", "false");
        assertThat(converter.from(multivaluedMap).isOnlyCount()).isFalse();
    }

    @Test
    public void shouldConvertSearchCondition() {
        multivaluedMap.addFirst("test[eq]", "value");
        assertThat(converter.from(multivaluedMap).getSearchConditions()).extracting("attribute", "operator", "values").contains(new Tuple("test:test", Operator.EQ, ImmutableList.of("value")));
    }

    @Test
    public void shouldConvertSearchConditionWithUnderscoreAndNumbers() {
        multivaluedMap.addFirst("test1_abc[eq]", "value");
        assertThat(converter.from(multivaluedMap).getSearchConditions()).extracting("attribute", "operator", "values").contains(new Tuple("test:test1_abc", Operator.EQ, ImmutableList.of("value")));
    }

    @Test
    public void shouldThrowInvalidSearchConditionBadSearchConditionAttribute() {
        multivaluedMap.addFirst("bad[eq]", "value");
        try {
            converter.from(multivaluedMap);
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid search condition attribute bad");
        }
    }

    @Test
    public void shouldThrowInvalidSearchConditionBadSearchConditionOperator() {
        multivaluedMap.addFirst("test[ne]", "value");
        try {
            converter.from(multivaluedMap);
            failBecauseExceptionWasNotThrown(InvalidSearchConditionException.class);
        } catch (InvalidSearchConditionException e) {
            assertThat(e).hasMessage("Invalid search condition operator NE for condition attribute test");
        }
    }

}