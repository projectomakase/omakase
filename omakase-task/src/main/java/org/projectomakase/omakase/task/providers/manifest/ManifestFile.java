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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * @author Richard Lucas
 */
public class ManifestFile {

    private static final String JSON_TEMPLATE_REQUIRED = "{\"uri\":\"%s\"}";
    private static final String JSON_TEMPLATE = "{\"uri\":\"%s\",\"size\":%d}";

    private final URI uri;
    private final long size;

    public ManifestFile(URI uri) {
        this.uri = uri;
        size = -1;
    }

    public ManifestFile(URI uri, long size) {
        this.uri = uri;
        this.size = size;
    }

    public URI getUri() {
        return uri;
    }

    public Optional<Long> getSize() {
        if (size != -1) {
            return Optional.of(size);
        } else {
            return Optional.empty();
        }
    }

    public String toJson() {
        return getSize()
                .map(fileSize -> String.format(JSON_TEMPLATE, uri.toString(), fileSize))
                .orElse(String.format(JSON_TEMPLATE_REQUIRED, uri.toString()));
    }

    public static ManifestFile fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            URI uri;
            long size = -1;

            if (!jsonObject.containsKey("uri")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'uri' property");
            } else {
                try {
                    uri = new URI(jsonObject.getString("uri"));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Invalid JSON, 'uri' is not a valid URI", e);
                }
            }

            if (jsonObject.containsKey("size")) {
                size = jsonObject.getInt("size");
            }

            return new ManifestFile(uri, size);

        }
    }

    @Override
    public String toString() {
        return "ManifestFile{" +
                "uri=" + uri +
                ", size='" + size + '\'' +
                '}';
    }
}
