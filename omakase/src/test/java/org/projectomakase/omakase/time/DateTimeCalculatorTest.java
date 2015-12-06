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
package org.projectomakase.omakase.time;

import org.junit.Test;

import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class DateTimeCalculatorTest {

    @Test
    public void shouldCalculateStartAndEndValuesForDate() {
        assertThat(DateTimeCalculator.calculateStartAndEndDates("2015-05-01").getStart().format(DateTimeFormatter.ISO_DATE_TIME)).isEqualTo("2015-05-01T00:00:00");
        assertThat(DateTimeCalculator.calculateStartAndEndDates("2015-05-01").getEnd().format(DateTimeFormatter.ISO_DATE_TIME)).isEqualTo("2015-05-01T23:59:59.999999999");
    }

    @Test
    public void shouldCalculateStartAndEndValuesForDateTime() {
        assertThat(DateTimeCalculator.calculateStartAndEndDates("2015-05-01T10:30").getStart().format(DateTimeFormatter.ISO_DATE_TIME)).isEqualTo("2015-05-01T10:30:00");
        assertThat(DateTimeCalculator.calculateStartAndEndDates("2015-05-01T10:30").getEnd().format(DateTimeFormatter.ISO_DATE_TIME)).isEqualTo("2015-05-01T10:30:59.999999999");
    }

}