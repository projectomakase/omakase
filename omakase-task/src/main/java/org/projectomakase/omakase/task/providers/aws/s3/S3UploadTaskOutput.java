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
package org.projectomakase.omakase.task.providers.aws.s3;

import org.projectomakase.omakase.commons.aws.s3.S3Part;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.spi.TaskOutput;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS Glacier Upload Task Output.
 *
 * @author Richard Lucas
 */
public class S3UploadTaskOutput implements TaskOutput {

    private static final String TASK_TYPE = "S3_UPLOAD";
    private static final String JSON_TEMPLATE = "{\"parts\":[%s],\"hashes\":[%s]}";

    private List<S3Part> parts;
    private List<Hash> hashes;

    public S3UploadTaskOutput() {
        // required by service loader
    }

    public S3UploadTaskOutput(List<S3Part> parts, List<Hash> hashes) {
        this.parts = parts;
        this.hashes = hashes;
    }

    public List<S3Part> getParts() {
        return parts;
    }

    public List<Hash> getHashes() {
        return hashes;
    }


    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, parts.stream().map(S3Part::toJson).collect(Collectors.joining(",")), hashes.stream().map(Hash::toJson).collect(Collectors.joining(",")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("parts")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'parts' property");
            } else {
                parts = jsonObject.getJsonArray("parts").stream().map(JsonValue::toString).map(S3Part.builder()::fromJson).collect(Collectors.toList());
            }

            if (!jsonObject.containsKey("hashes")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'hashes' property");
            } else {
                hashes = jsonObject.getJsonArray("hashes").stream().map(JsonValue::toString).map(Hash.builder()::fromJson).collect(Collectors.toList());
            }
        }
    }

    @Override
    public String toString() {
        return "S3UploadTaskOutput{" +
                "parts=" + parts +
                ", hashes=" + hashes +
                '}';
    }
}
