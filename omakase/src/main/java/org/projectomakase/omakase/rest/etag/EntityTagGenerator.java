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
package org.projectomakase.omakase.rest.etag;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import javax.ws.rs.core.EntityTag;

/**
 * Utility methods for generating ETags
 *
 * @author Richard Lucas
 */
public final class EntityTagGenerator {

    private static final HashFunction HASH_FUNCTION = Hashing.sha256();

    private EntityTagGenerator() {
        // utility class
    }

    /**
     * Generates a ETag from a long value
     *
     * @param value
     *         the long value
     * @return the generated ETag.
     */
    public static EntityTag entityTagFromLong(long value) {
        return new EntityTag(HASH_FUNCTION.newHasher().putLong(value).hash().toString());
    }

    /**
     * Generates a ETag from a string value. Assumes UTF-8 encoding.
     *
     * @param value
     *         the string value
     * @return the generated ETag.
     */
    public static EntityTag entityTagFromString(String value) {
        return new EntityTag(HASH_FUNCTION.newHasher().putString(value, Charsets.UTF_8).hash().toString());
    }
}
