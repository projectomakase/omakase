/*
 * #%L
 * omakase-task
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
package org.projectomakase.omakase.task.providers.hash;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a HashInput
 *
 * @author Richard Lucas
 */
public class HashInput {

    private static final String ALGORITHM_JSON_TEMPLATE = "\"hash_algorithm\":\"%s\"";
    private static final String OFFSET_JSON_TEMPLATE = "\"offset\":%d";
    private static final String LENGTH_JSON_TEMPLATE = "\"length\":%d";

    private final String algorithm;
    private final Long offset;
    private final Long length;

    /**
     * Creates a new hash input without an offset or length.
     *
     * @param algorithm
     *         the name of the hash algorithm
     */
    public HashInput(String algorithm) {
        this.algorithm = algorithm;
        this.offset = null;
        this.length = null;
    }

    /**
     * Creates a new hash input with an offset
     *
     * @param algorithm
     *         the name of the hash algorithm
     * @param offset
     *         the offset to the byte range the hash should be calculated from
     */
    public HashInput(String algorithm, long offset) {
        this.algorithm = algorithm;
        this.offset = offset;
        this.length = null;
    }

    /**
     * Creates a new hash input with an offset and length
     *
     * @param algorithm
     *         the name of the hash algorithm
     * @param offset
     *         the offset to the byte range the hash should be calculated from
     * @param length
     *         the length of the byte range that the hash should be calculated for.
     * @throws IllegalArgumentException
     *         if the offset and length and not valid values.
     */
    public HashInput(String algorithm, long offset, long length) {
        this.algorithm = algorithm;
        this.offset = offset;
        this.length = length;
        validateOffsetAndLength(offset, length);
    }

    /**
     * Returns the name of the hash algorithm.
     *
     * @return the name of the hash algorithm.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the offset to the byte range the hash should be calculated from, or an empty optional if no offset was specified.
     * <p>
     * It can be assumed that the hash offset is 0 if an empty optional is returned.
     * </p>
     *
     * @return the offset to the byte range the hash should be calculated from, or an empty optional if no offset was specified.
     */
    public Optional<Long> getOffset() {
        return Optional.ofNullable(offset);
    }

    /**
     * Returns the length of the byte range that the hash should be calculated for, or an empty optional if no length was specified.
     * <p>
     * It can be assumed that the hash is for the entire data set if an empty optional is returned.
     * </p>
     *
     * @return the length of the byte range that the hash should be calculated for, or an empty optional if no length was specified.
     */
    public Optional<Long> getLength() {
        return Optional.ofNullable(length);
    }

    /**
     * Serializes the hash input into a JSON string.
     * <p>
     * If a offset and length are not specified the JSON does not include these values.
     * </p>
     *
     * @return a JSON representation of the hash.
     */
    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format(ALGORITHM_JSON_TEMPLATE, algorithm));
        getOffset().ifPresent(from -> attributes.add(String.format(OFFSET_JSON_TEMPLATE, from)));
        getLength().ifPresent(to -> attributes.add(String.format(LENGTH_JSON_TEMPLATE, to)));
        return "{" + String.join(",", attributes) + "}";
    }

    @Override
    public String toString() {
        return "HashInput{" +
                "algorithm='" + algorithm + '\'' +
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
         * Deserializes a JSON string into a {@link HashInput}.
         *
         * @param json
         *         the JSON string.
         * @return the hash.
         */
        public HashInput fromJson(String json) {
            try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
                JsonObject jsonObject = jsonReader.readObject();

                if (!jsonObject.containsKey("hash_algorithm")) {
                    throw new IllegalArgumentException("Invalid JSON, requires a 'hash_algorithm' property");
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
                    return new HashInput(jsonObject.getString("hash_algorithm"));
                } else if (hashLength == -1) {
                    return new HashInput(jsonObject.getString("hash_algorithm"), hashOffset);
                } else {
                    return new HashInput(jsonObject.getString("hash_algorithm"), hashOffset, hashLength);
                }
            }
        }
    }
}
