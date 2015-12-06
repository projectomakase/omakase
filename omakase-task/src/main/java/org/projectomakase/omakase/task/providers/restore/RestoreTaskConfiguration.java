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
package org.projectomakase.omakase.task.providers.restore;

import org.projectomakase.omakase.task.spi.TaskConfiguration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Restore Task Configuration.
 *
 * @author Richard Lucas
 */
public class RestoreTaskConfiguration implements TaskConfiguration {

    private static final String TASK_TYPE = "RESTORE";
    private static final String JSON_TEMPLATE = "{\"source\":\"%s\"}";

    private URI source;


    public RestoreTaskConfiguration() {
        // required by service loader
    }

    public RestoreTaskConfiguration(URI source) {
        this.source = source;
    }

    public URI getSource() {
        return source;
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, source);
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            this.source = getURIProperty(jsonObject, "source");
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
        return "RestoreTaskConfiguration{" +
                "source=" + source +
                '}';
    }
}
