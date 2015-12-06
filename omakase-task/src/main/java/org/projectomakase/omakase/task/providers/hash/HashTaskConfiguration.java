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

import org.projectomakase.omakase.task.spi.TaskConfiguration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hash Task Configuration.
 *
 * @author Richard Lucas
 */
public class HashTaskConfiguration implements TaskConfiguration {

    private static final String TASK_TYPE = "HASH";
    private static final String JSON_TEMPLATE = "{\"source\":\"%s\",\"hashes\":[%s]}";

    private URI source;
    private List<HashInput> hashes;

    public HashTaskConfiguration() {
        // required by service loader
    }

    public HashTaskConfiguration(URI source, List<HashInput> hashes) {
        this.source = source;
        this.hashes = hashes;
    }

    public URI getSource() {
        return source;
    }

    public List<HashInput> getHashes() {
        return hashes;
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, source, hashes.stream().map(HashInput::toJson).collect(Collectors.joining(",")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            this.source = getURIProperty(jsonObject, "source");

            if (!jsonObject.containsKey("hashes")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'hashes' property");
            }

            List<HashInput> hashInputList =
                    jsonObject.getJsonArray("hashes").stream().map(JsonValue::toString).map(HashInput.builder()::fromJson).collect(Collectors.toList());
            this.hashes = hashInputList;

        }
    }

    private static URI getURIProperty(JsonObject jsonObject, String key) {
        if (!jsonObject.containsKey(key)) {
            throw new IllegalArgumentException("Invalid JSON, requires a '" + key + "' property");
        } else {
            try {
                return new URI(jsonObject.getString(key));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid JSON, '" + key + "' is not a valid URI", e);
            }
        }
    }

    @Override
    public String toString() {
        return "HashTaskConfiguration{" +
                "source=" + source +
                ", hashes=" + hashes +
                '}';
    }
}
