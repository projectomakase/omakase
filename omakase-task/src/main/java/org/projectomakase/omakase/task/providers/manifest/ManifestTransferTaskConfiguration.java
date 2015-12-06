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
package org.projectomakase.omakase.task.providers.manifest;

import org.projectomakase.omakase.task.spi.TaskConfiguration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Richard Lucas
 */
public class ManifestTransferTaskConfiguration implements TaskConfiguration {

    private static final String TASK_TYPE = "MANIFEST_TRANSFER";
    private static final String JSON_TEMPLATE = "{\"source\":\"%s\",\"destination\":\"%s\",\"hash_algorithms\":[%s]}";

    private URI source;
    private URI destination;
    private List<String> hashAlgorithms;

    public ManifestTransferTaskConfiguration() {
        // required by service loader
    }

    public ManifestTransferTaskConfiguration(URI source, URI destination, List<String> hashAlgorithms) {
        this.source = source;
        this.destination = destination;
        this.hashAlgorithms = hashAlgorithms;
    }

    public URI getSource() {
        return source;
    }

    public URI getDestination() {
        return destination;
    }

    public List<String> getHashAlgorithms() {
        return hashAlgorithms;
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, source, destination, hashAlgorithms.stream().map(algorithm -> "\"" + algorithm + "\"").collect(Collectors.joining(",")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            this.source = getURIProperty(jsonObject, "source");
            this.destination = getURIProperty(jsonObject, "destination");

            if (jsonObject.containsKey("hash_algorithms")) {
                hashAlgorithms = jsonObject.getJsonArray("hash_algorithms").stream().map(value -> ((JsonString) value).getString()).collect(Collectors.toList());
            } else {
                hashAlgorithms = Collections.emptyList();
            }
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
        return "ManifestTransferTaskConfiguration{" +
                "source=" + source +
                ", destination=" + destination +
                ", hashAlgorithms=" + hashAlgorithms +
                '}';
    }
}
