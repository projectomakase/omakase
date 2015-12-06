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
package org.projectomakase.omakase.task.providers.aws;

import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.task.spi.TaskConfiguration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS Upload Task Configuration.
 *
 * @author Richard Lucas
 */
public abstract class AWSUploadTaskConfiguration implements TaskConfiguration {

    private static final String JSON_TEMPLATE = "{\"source\":\"%s\",\"destination\":\"%s\",\"part_size\":%d,\"parts\":[%s],\"hash_algorithms\":[%s]}";

    private URI source;
    private URI destination;
    private long partSize;
    private List<AWSUploadPart> parts;
    private List<String> hashAlgorithms;

    public AWSUploadTaskConfiguration() {
        // required by service loader
    }

    public AWSUploadTaskConfiguration(URI source, URI destination, long partSize, List<AWSUploadPart> parts, List<String> hashAlgorithms) {
        this.source = source;
        this.destination = destination;
        this.partSize = partSize;
        this.parts = parts;
        this.hashAlgorithms = hashAlgorithms;
    }

    public URI getSource() {
        return source;
    }

    public URI getDestination() {
        return destination;
    }

    public long getPartSize() {
        return partSize;
    }

    public List<AWSUploadPart> getParts() {
        return parts;
    }

    public List<String> getHashAlgorithms() {
        return hashAlgorithms;
    }

    @Override
    public String toJson() {
        String partsJson = parts.stream().map(AWSUploadPart::toJson).collect(Collectors.joining(","));
        String hashAlgorithmsJson = hashAlgorithms.stream().map(algorithm -> "\"" + algorithm + "\"").collect(Collectors.joining(","));
        return String.format(JSON_TEMPLATE, source, destination, partSize, partsJson, hashAlgorithmsJson);
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            this.source = getURIProperty(jsonObject, "source");
            this.destination = getURIProperty(jsonObject, "destination");

            if (!jsonObject.containsKey("part_size")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'part_size' property");
            } else {
                partSize = jsonObject.getJsonNumber("part_size").longValue();
            }


            if (!jsonObject.containsKey("parts")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'parts' property");
            }

            parts = jsonObject.getJsonArray("parts").stream().map(JsonValue::toString).map(partJson -> AWSUploadPart.builder().fromJson(partJson)).collect(Collectors.toList());

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
}
