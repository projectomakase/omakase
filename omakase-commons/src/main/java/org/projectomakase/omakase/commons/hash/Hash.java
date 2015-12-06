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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Optional;

/**
 * Represents a hash.
 *
 * @author Richard Lucas
 */
public class Hash {

    private static final String SIMPLE_JSON_TEMPLATE = "{\"hash_algorithm\":\"%s\",\"hash\":\"%s\"}";
    private static final String JSON_TEMPLATE = "{\"hash_algorithm\":\"%s\",\"offset\":%d,\"length\":%d,\"hash\":\"%s\"}";

    private final String algorithm;
    private final String value;
    private final long offset;
    private final Long length;

    /**
     * Creates a new hash without an offset or length.
     *
     * @param algorithm
     *         the name of the hash algorithm used to create the hash
     * @param value
     *         the hash value
     */
    public Hash(String algorithm, String value) {
        this.algorithm = algorithm;
        this.value = value;
        this.offset = 0;
        this.length = null;
    }

    /**
     * Creates a new hash with an offset and length
     *
     * @param algorithm
     *         the name of the hash algorithm used to create the hash
     * @param value
     *         the hash value
     * @param offset
     *         the offset the hash was calculated from
     * @param length
     *         the length of the byte range the hash was calculated for.
     * @throws IllegalArgumentException
     *         if the offset and length and not valid values.
     */
    public Hash(String algorithm, String value, long offset, long length) {
        this.algorithm = algorithm;
        this.value = value;
        this.offset = offset;
        this.length = length;
        validateOffsetAndLength(offset, length);
    }

    /**
     * Returns the name of the hash algorithm used to create the hash.
     *
     * @return the name of the hash algorithm used to create the hash.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the value of the calculated hash.
     *
     * @return the value of the calculated hash.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the offset the hash was calculated from.
     *
     * @return the offset the hash was calculated from.
     */
    public Long getOffset() {
        return offset;
    }

    /**
     * Returns the length of the byte range the hash was calculated for, or an empty optional if no length was specified.
     * <p>
     * It can be assumed that the hash is for the entire data set if an empty optional is returned.
     * </p>
     *
     * @return the length of the byte range the hash was calculated for, or an empty optional if no length was specified.
     */
    public Optional<Long> getLength() {
        return Optional.ofNullable(length);
    }

    /**
     * Serializes the hash into a JSON string.
     * <p>
     * If a offset and length are not specified the JSON does not include these values.
     * </p>
     *
     * @return a JSON representation of the hash.
     */
    public String toJson() {
        if (length == null) {
            return String.format(SIMPLE_JSON_TEMPLATE, algorithm, value);
        } else {
            return String.format(JSON_TEMPLATE, algorithm, offset, length, value);
        }

    }

    @Override
    public String toString() {
        return "Hash{" +
                "algorithm='" + algorithm + '\'' +
                ", value='" + value + '\'' +
                ", offset=" + offset +
                ", length=" + length +
                '}';
    }

    private static void validateOffsetAndLength(long offset, long length) {
        if (offset < 0) {
            throw new IllegalArgumentException("the offset can not be less than 0");
        }
        if (length <= offset) {
            throw new IllegalArgumentException("the length must be greater than the offset");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /**
         * Deserializes a JSON string into a {@link Hash}.
         *
         * @param json
         *         the JSON string.
         * @return the hash.
         */
        public Hash fromJson(String json) {
            try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
                JsonObject jsonObject = jsonReader.readObject();

                if (!jsonObject.containsKey("hash_algorithm")) {
                    throw new IllegalArgumentException("Invalid JSON, requires a 'hash_algorithm' property");
                }

                if (!jsonObject.containsKey("hash")) {
                    throw new IllegalArgumentException("Invalid JSON, requires a 'hash' property");
                }

                long hashOffset = -1;
                long hashLength = -1;

                if (jsonObject.containsKey("offset")) {
                    hashOffset = jsonObject.getInt("offset");
                }

                if (jsonObject.containsKey("length")) {
                    hashLength = jsonObject.getInt("length");
                }

                if (hashOffset == -1 && hashLength == -1) {
                    return new Hash(jsonObject.getString("hash_algorithm"), jsonObject.getString("hash"));
                } else {
                    return new Hash(jsonObject.getString("hash_algorithm"), jsonObject.getString("hash"), hashOffset, hashLength);
                }
            }
        }
    }
}
