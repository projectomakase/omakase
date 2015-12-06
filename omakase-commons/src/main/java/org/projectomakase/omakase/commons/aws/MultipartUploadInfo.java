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
import javax.json.JsonString;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The multi part upload information required to calculate the upload parts used to perform an upload to AWS.
 *
 * @author Richard Lucas
 */
public class MultipartUploadInfo {

    private static final String PART_SIZE = "part_size";
    private static final String REQUIRED_HASH_ALGORITHMS = "required_hash_algorithms";
    private static final String JSON_TEMPLATE = "{\"" + PART_SIZE + "\":%d,\"" + REQUIRED_HASH_ALGORITHMS + "\":[%s]}";

    private final long partSize;
    private final List<String> requiredHashAlgorithms;

    /**
     * Creates a new {@link MultipartUploadInfo}.
     *
     * @param partSize the part size used to calculate the upload parts.
     * @param requiredHashAlgorithms a list of hash algorithms that need to be calculated for each part.
     */
    public MultipartUploadInfo(long partSize, List<String> requiredHashAlgorithms) {
        checkArgument(partSize > 0, "partSize must be greater than 0");
        checkArgument(requiredHashAlgorithms != null, "requiredHashAlgorithms must not be null");
        checkArgument(!requiredHashAlgorithms.isEmpty(), "requiredHashAlgorithms must not be empty");
        this.partSize = partSize;
        this.requiredHashAlgorithms = requiredHashAlgorithms;
    }

    /**
     * Returns the part size used to calculate the upload parts.
     *
     * @return the part size used to calculate the upload parts.
     */
    public long getPartSize() {
        return partSize;
    }

    /**
     * Returns a list of hash algorithms that need to be calculated for each part.
     *
     * @return a list of hash algorithms that need to be calculated for each part.
     */
    public List<String> getRequiredHashAlgorithms() {
        return requiredHashAlgorithms;
    }

    public String toJson() {
        return String.format(JSON_TEMPLATE, partSize,  requiredHashAlgorithms.stream().map(algorithm -> "\"" + algorithm + "\"").collect(Collectors.joining(",")));
    }

    public static MultipartUploadInfo fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            long size;
            if (!jsonObject.containsKey(PART_SIZE)) {
                throw new IllegalArgumentException("Invalid JSON, requires a '" + PART_SIZE + "' property");
            } else {
                size = jsonObject.getJsonNumber(PART_SIZE).longValue();
            }

            List<String> hashAlgorithms;
            if (!jsonObject.containsKey(REQUIRED_HASH_ALGORITHMS)) {
                throw new IllegalArgumentException("Invalid JSON, requires a '" + REQUIRED_HASH_ALGORITHMS + "' property");
            } else {
                hashAlgorithms = jsonObject.getJsonArray(REQUIRED_HASH_ALGORITHMS).stream().map(value -> ((JsonString) value).getString()).collect(Collectors.toList());
            }

            return new MultipartUploadInfo(size, hashAlgorithms);
        }
    }

    @Override
    public String toString() {
        return "MultipartUploadInfo{" +
                "partSize=" + partSize +
                ", requiredHashAlgorithms=" + requiredHashAlgorithms +
                '}';
    }
}
