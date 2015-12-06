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

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * @author Richard Lucas
 */
public final class DateTimeFormatters {

    private DateTimeFormatters() {
        // hide default constructor
    }

    /**
     * Returns the default Omakase Zoned DateTime Formatter.
     * <p>
     *     Format YYYY-MM-DDTHH:MM:SS
     * </p>
     *
     * @return the default Omakase Zoned DateTime Formatter.
     */
    public static DateTimeFormatter getDefaultZonedDateTimeFormatter() {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T').appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).appendOffsetId().toFormatter();
    }
}
