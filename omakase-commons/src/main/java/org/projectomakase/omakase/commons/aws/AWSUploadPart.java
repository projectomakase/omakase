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
package org.projectomakase.omakase.commons.aws;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a AWS Glacier Upload part.
 *
 * @author Richard Lucas
 */
public class AWSUploadPart {

    private static final String INVALID_JSON = "Invalid JSON, requires a '%s' property";

    private static final String JSON_TEMPLATE = "{\"number\":%d,\"offset\":%d,\"length\":%d,\"signing_hash\":\"%s\",\"part_hash\":\"%s\"}";

    private final int number;
    private final long offset;
    private final long length;
    private final String signingHash;
    private final String partHash;

    public AWSUploadPart(int number, long offset, long length, String signingHash, String partHash) {
        this.number = number;
        this.offset = offset;
        this.length = length;
        this.signingHash = signingHash;
        this.partHash = partHash;
    }

    public int getNumber() {
        return number;
    }

    public long getOffset() {
        return offset;
    }

    public long getLength() {
        return length;
    }

    public String getSigningHash() {
        return signingHash;
    }

    public String getPartHash() {
        return partHash;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Serializes the glacier upload part into a JSON string.
     *
     * @return a JSON representation of the glacier upload part.
     */
    public String toJson() {
        return String.format(JSON_TEMPLATE, number, offset, length, signingHash, partHash);
    }

    public static class Builder {

        /**
         * Deserializes a JSON string into a {@link AWSUploadPart}.
         *
         * @param json
         *         the JSON string.
         * @return the {@link AWSUploadPart}.
         */
        public AWSUploadPart fromJson(String json) {
            try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
                JsonObject jsonObject = jsonReader.readObject();
                checkArgument(jsonObject.containsKey("number"), String.format(INVALID_JSON, "number"));
                checkArgument(jsonObject.containsKey("offset"), String.format(INVALID_JSON, "offset"));
                checkArgument(jsonObject.containsKey("length"), String.format(INVALID_JSON, "length"));
                checkArgument(jsonObject.containsKey("signing_hash"), String.format(INVALID_JSON, "signing_hash"));
                checkArgument(jsonObject.containsKey("part_hash"), String.format(INVALID_JSON, "part_hash"));
                return new AWSUploadPart(jsonObject.getInt("number"), jsonObject.getInt("offset"), jsonObject.getInt("length"), jsonObject.getString("signing_hash"),
                        jsonObject.getString("part_hash"));
            }
        }
    }

    @Override
    public String toString() {
        return "GlacierUploadPart{" +
                "number=" + number +
                ", offset=" + offset +
                ", length=" + length +
                ", signingHash='" + signingHash + '\'' +
                ", partHash='" + partHash + '\'' +
                '}';
    }
}
