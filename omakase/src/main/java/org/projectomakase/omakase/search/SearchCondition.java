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

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents a single search condition.
 * <p>
 * Each search condition value can be either a single value or a comma separated list of values. Comma separated values represent OR conditions. It is currently up to the implementation parsing the
 * conditions to handle this. (TODO: Provide a cleaner structure for holding OR values)
 * </p>
 *
 * @author Richard Lucas
 */
public class SearchCondition {

    private final String attribute;
    private final Operator operator;
    private final List<String> values;
    private final boolean isDate;

    public SearchCondition(@NotNull String attribute, @NotNull Operator operator, @NotNull String value) {
        this.attribute = attribute;
        this.operator = operator;
        this.values = ImmutableList.of(value);
        this.isDate = false;
    }

    public SearchCondition(@NotNull String attribute, @NotNull Operator operator, @NotNull List<String> values) {
        this.attribute = attribute;
        this.operator = operator;
        this.values = values;
        this.isDate = false;
    }

    public SearchCondition(@NotNull String attribute, @NotNull Operator operator, @NotNull String value, boolean isDate) {
        this.attribute = attribute;
        this.operator = operator;
        this.values = ImmutableList.of(value);
        this.isDate = isDate;
    }

    public SearchCondition(String attribute, Operator operator, List<String> values, boolean isDate) {
        this.attribute = attribute;
        this.operator = operator;
        this.values = values;
        this.isDate = isDate;
    }

    public String getAttribute() {
        return attribute;
    }

    public Operator getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean isDate() {
        return isDate;
    }

    @Override
    public String toString() {
        return "SearchCondition{" +
                "attribute='" + attribute + '\'' +
                ", operator=" + operator +
                ", values=" + values +
                ", isDate=" + isDate +
                '}';
    }
}
