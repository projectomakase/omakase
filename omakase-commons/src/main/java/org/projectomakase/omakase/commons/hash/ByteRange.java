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
package org.projectomakase.omakase.commons.hash;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a byte range.
 *
 * @author Richard Lucas
 */
public class ByteRange {

    private final long from;
    private final long to;

    public ByteRange(long from, long to) {
        checkArgument(from < to, "\"from\" should be lower than \"to\"");
        checkArgument(from >= 0 && to > 0, "\"from\" cannot be negative and \"to\" has to be positive");
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the start of the content range in bytes.
     *
     * @return the start of the content range in bytes.
     */
    public long getFrom() {
        return from;
    }

    /**
     * Returns the end of the content range in bytes.
     *
     * @return the end of the content range in bytes.
     */
    public long getTo() {
        return to;
    }

    @Override
    public String toString() {
        return from + "-" + to;
    }
}
