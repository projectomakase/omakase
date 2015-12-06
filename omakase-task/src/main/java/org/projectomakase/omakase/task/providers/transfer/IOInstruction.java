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
package org.projectomakase.omakase.task.providers.transfer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Transfer IO Instructions.
 *
 * @author Richard Lucas
 */
public class IOInstruction {

    private static final String JSON_TEMPLATE = "{\"source\":\"%s\",\"destination\":\"%s\"}";

    private final URI source;
    private final URI destination;

    public IOInstruction(URI source, URI destination) {
        this.source = source;
        this.destination = destination;
    }

    public URI getSource() {
        return source;
    }

    public URI getDestination() {
        return destination;
    }

    public String toJson() {
        return String.format(JSON_TEMPLATE, source, destination);
    }

    public static IOInstruction fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            return new IOInstruction(getURIProperty(jsonObject, "source"), getURIProperty(jsonObject, "destination"));
        }
    }

    @Override
    public String toString() {
        return "IOInstruction{" +
                "source=" + source +
                ", destination=" + destination +
                '}';
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
