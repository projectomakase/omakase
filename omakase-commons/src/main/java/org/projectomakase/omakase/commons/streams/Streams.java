/*
 * #%L
 * omakase-commons
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
package org.projectomakase.omakase.commons.streams;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Provides additional functionality missing from the Stream API.
 *
 * @author Richard Lucas
 */
public final class Streams {

    private Streams() {
        // Hide public constructor
    }

    /**
     * Converts an Optional's value to a Stream. If the Optional is empty an empty Stream is returned.
     *
     * @param optional
     *         the optional
     * @param <T>
     *         the type of the optional's value
     * @return a Stream containing the Optional's value or an empty Stream if the Optional was empty
     */
    public static <T> Stream<? extends T> optionalToStream(Optional<T> optional) {
        return optional.map(Stream::of).orElseGet(Stream::empty);
    }
}
