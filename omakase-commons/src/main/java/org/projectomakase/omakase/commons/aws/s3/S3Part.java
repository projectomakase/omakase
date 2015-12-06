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
package org.projectomakase.omakase.commons.aws.s3;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

/**
 * @author Richard Lucas
 */
public class S3Part {

    private static final String JSON_TEMPLATE = "{\"number\":%d,\"etag\":\"%s\"}";

    private final int number;
    private final String etag;

    public S3Part(int number, String etag) {
        this.number = number;
        this.etag = etag;
    }

    public int getNumber() {
        return number;
    }

    public String getEtag() {
        return etag;
    }

    public String toJson() {
        return String.format(JSON_TEMPLATE, number, etag);
    }

    @Override
    public String toString() {
        return "S3Part{" +
                "number=" + number +
                ", etag='" + etag + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /**
         * Deserializes a JSON string into a {@link S3Part}.
         *
         * @param json
         *         the JSON string.
         * @return the hash.
         */
        public S3Part fromJson(String json) {
            try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
                JsonObject jsonObject = jsonReader.readObject();

                if (!jsonObject.containsKey("number")) {
                    throw new IllegalArgumentException("Invalid JSON, requires a 'number' property");
                }

                if (!jsonObject.containsKey("etag")) {
                    throw new IllegalArgumentException("Invalid JSON, requires a 'etag' property");
                }

                return new S3Part(jsonObject.getInt("number"), jsonObject.getString("etag"));
            }
        }
    }
}
