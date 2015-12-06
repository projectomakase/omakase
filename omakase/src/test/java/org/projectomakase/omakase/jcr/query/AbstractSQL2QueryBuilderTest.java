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
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SearchException;
import org.projectomakase.omakase.search.SortOrder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class AbstractSQL2QueryBuilderTest {

    @Test
    public void shouldGetOrderByEmptyOptional() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getOrderBy(null, null)).isEmpty();
    }

    @Test
    public void shouldGetOrderBy() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getOrderBy("test", SortOrder.ASC)).isPresent().contains(" ORDER by node.[test] " + SortOrder.ASC.name());
    }

    @Test
    public void shouldGetLimitEmptyOptional() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getLimit(null, null)).isEmpty();
    }

    @Test
    public void shouldGetLimit() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getLimit(10, 0)).isPresent().contains(" LIMIT 10 OFFSET 0");
    }


    @Test
    public void shouldGetJcrSql2Conditions() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSql2Conditions(ImmutableList.of(new SearchCondition("test:attr1", Operator.EQ, "value1,value2,value3", false))))
                .containsExactly(" AND (node.[test:attr1] = 'value1' OR node.[test:attr1] = 'value2' OR node.[test:attr1] = 'value3')");
    }

    @Test
    public void shouldGetJcrSql2ConditionsForValue() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSql2ConditionsForValue(new SearchCondition("test:attr1", Operator.EQ, "value1,value2,value3", false)))
                .containsExactly(" AND (node.[test:attr1] = 'value1' OR node.[test:attr1] = 'value2' OR node.[test:attr1] = 'value3')");
    }

    @Test
    public void shouldGetJcrSql2Condition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSql2Condition("test:attr1", Operator.EQ, "value1,value2,value3", false))
                .isEqualTo(" AND (node.[test:attr1] = 'value1' OR node.[test:attr1] = 'value2' OR node.[test:attr1] = 'value3')");
    }

    @Test
    public void shouldGetJcrSql2DateCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSql2Condition("test:attr1", Operator.EQ, "2015-05-01", true))
                .isEqualTo(" AND (node.[test:attr1] BETWEEN '2015-05-01T00:00:00' AND '2015-05-01T23:59:59.999999999')");
    }

    @Test
    public void shouldGetJcrSql2LikeCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSql2Condition("test:attr1", Operator.LIKE, "value1", false)).isEqualTo(" AND (node.[test:attr1] LIKE '%value1%')");
    }

    @Test
    public void shouldGetEqualDateCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getDateCondition("test", Operator.EQ, "2015-05-01")).isEqualTo("node.[test] BETWEEN '2015-05-01T00:00:00' AND '2015-05-01T23:59:59.999999999' OR ");
    }

    @Test
    public void shouldGetGreaterThanDateCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getDateCondition("test", Operator.GT, "2015-05-01")).isEqualTo("node.[test] > '2015-05-01T23:59:59.999999999' OR ");
    }

    @Test
    public void shouldGetGreaterThanOrEqualDateCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getDateCondition("test", Operator.GTE, "2015-05-01")).isEqualTo("node.[test] >= '2015-05-01T00:00:00' OR ");
    }

    @Test
    public void shouldGetLessThanDateCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getDateCondition("test", Operator.LT, "2015-05-01")).isEqualTo("node.[test] < '2015-05-01T00:00:00' OR ");
    }

    @Test
    public void shouldGetLessThanOrEqualDateCondition() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getDateCondition("test", Operator.LTE, "2015-05-01")).isEqualTo("node.[test] <= '2015-05-01T23:59:59.999999999' OR ");
    }

    @Test
    public void shouldThrowSearchExceptionUnsupportedDateOperator() throws Exception {
        assertThatThrownBy(() -> AbstractSQL2QueryBuilder.getDateCondition("test", Operator.LIKE, "2015-05-01")).isExactlyInstanceOf(SearchException.class)
                .hasMessage("Error generating Jcr SQL2 query. Unsupported date operator LIKE");
    }

    @Test
    public void shouldGetEqualComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.EQ)).isEqualTo("=");
    }

    @Test
    public void shouldGetNotEqualComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.NE)).isEqualTo("!=");
    }

    @Test
    public void shouldGetLikeComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.LIKE)).isEqualTo("LIKE");
    }

    @Test
    public void shouldGetGreaterThanComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.GT)).isEqualTo(">");
    }

    @Test
    public void shouldGetGreaterThanOrEqualComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.GTE)).isEqualTo(">=");
    }

    @Test
    public void shouldGetLessThanComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.LT)).isEqualTo("<");
    }

    @Test
    public void shouldGetLessThanOrEqualComparisonConstraint() throws Exception {
        assertThat(AbstractSQL2QueryBuilder.getJcrSQL2ComparisonConstraint(Operator.LTE)).isEqualTo("<=");
    }

}