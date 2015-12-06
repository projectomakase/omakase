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

/**
 * @author Richard Lucas
 */
public class Manifest {

    private static final String JSON_TEMPLATE = "{\"uri\":\"%s\",\"description\":\"%s\"}";

    private final URI uri;
    private final String description;

    public Manifest(URI uri, String description) {
        this.uri = uri;
        this.description = description;
    }

    public URI getUri() {
        return uri;
    }

    public String getDescription() {
        return description;
    }

    public String toJson() {
        return String.format(JSON_TEMPLATE, uri.toString(), description);
    }

    public static Manifest fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            URI uri;
            String description;

            if (!jsonObject.containsKey("uri")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'uri' property");
            } else {
                try {
                    uri = new URI(jsonObject.getString("uri"));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Invalid JSON, 'uri' is not a valid URI", e);
                }
            }

            if (!jsonObject.containsKey("description")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'description' property");
            } else {
                description = jsonObject.getString("description");
            }

            return new Manifest(uri, description);

        }
    }

    @Override
    public String toString() {
        return "Manifest{" +
                "uri=" + uri +
                ", description='" + description + '\'' +
                '}';
    }
}
