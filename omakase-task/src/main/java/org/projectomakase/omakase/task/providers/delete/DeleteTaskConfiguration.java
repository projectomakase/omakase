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
package org.projectomakase.omakase.task.providers.delete;

import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.task.spi.TaskConfiguration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delete Task Configuration.
 *
 * @author Richard Lucas
 */
public class DeleteTaskConfiguration implements TaskConfiguration {

    private static final String TASK_TYPE = "DELETE";
    private static final String JSON_TEMPLATE = "{\"locations\":[%s]}";

    private List<URI> locations;


    public DeleteTaskConfiguration() {
        // required by service loader
    }

    public DeleteTaskConfiguration(List<URI> locations) {
        this.locations = locations;
    }

    public List<URI> getLocations() {
        return locations;
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, locations.stream().map(location -> "\"" + location + "\"").collect(Collectors.joining(",")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            this.locations = jsonObject.getJsonArray("locations").stream().map(value -> ((JsonString) value).getString()).map(location -> Throwables
                    .returnableInstance(() -> new URI(location))).collect(Collectors.toList());
        }
    }

    @Override
    public String toString() {
        return "DeleteTaskConfiguration{" +
                "locations=" + locations +
                '}';
    }
}
