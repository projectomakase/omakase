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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Java 8 DateTime calculator
 *
 * @author Richard Lucas
 */
public final class DateTimeCalculator {

    private DateTimeCalculator() {
        // static methods only
    }

    /**
     * Calculates the start and end date/time for a given string value. The string value must confirm to either the ISO_LOCAL_DATE format or the ISO_LOCAL_DATE_TIME format.
     * <p>
     * If the value is a date the start value is returned as the start of the day and end value is returned as the end of the day. If the value is a date/time the start value is
     * returned as the start of the minute and the end value is returned as the end of the minute.
     * </p>
     *
     * @param value
     *         a string value must confirm to either the ISO_LOCAL_DATE format or the ISO_LOCAL_DATE_TIME format
     * @return a {@link DateTimeStartEnd} containing the start and end values.
     */
    public static DateTimeStartEnd calculateStartAndEndDates(String value) {
        LocalDateTime start;
        LocalDateTime end;
        if (value.contains("T")) {
            LocalDateTime dateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            start = dateTime.withSecond(0).withNano(0);
            end = dateTime.withSecond(59).withNano(999999999);
        } else {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            start = date.atStartOfDay();
            end = date.atTime(23, 59, 59, 999999999);
        }
        return new DateTimeStartEnd(start, end);
    }

}
