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
 * Transfer Task Output.
 *
 * @author Richard Lucas
 */
public class HashTaskOutput implements TaskOutput {

    private static final String TASK_TYPE = "HASH";
    private static final String JSON_TEMPLATE = "{\"hashes\":[%s]}";

    private List<Hash> hashes;

    public HashTaskOutput() {
        // required by service loader
    }

    public HashTaskOutput(List<Hash> hashes) {
        this.hashes = hashes;
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
        return String.format(JSON_TEMPLATE, hashes.stream().map(Hash::toJson).collect(Collectors.joining(", ")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("hashes")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'hashes' property");
            } else {
                hashes = jsonObject.getJsonArray("hashes").stream().map(JsonValue::toString).map(Hash.builder()::fromJson).collect(Collectors.toList());
            }
        }
    }

    @Override
    public String toString() {
        return "HashTaskOutput{" +
                "hashes=" + hashes +
                '}';
    }
}
