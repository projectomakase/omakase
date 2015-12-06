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
public class TransferTaskOutput implements TaskOutput {

    private static final String TASK_TYPE = "TRANSFER";
    private static final String JSON_TEMPLATE = "{\"content_info\":[%s]}";

    private List<ContentInfo> contentInfos;

    public TransferTaskOutput() {
        // required by service loader
    }

    public TransferTaskOutput(List<ContentInfo> contentInfos) {
        this.contentInfos = contentInfos;
    }

    public List<ContentInfo> getContentInfos() {
        return contentInfos;
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, contentInfos.stream().map(ContentInfo::toJson).collect(Collectors.joining(", ")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("content_info")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'content_info' property");
            } else {
                contentInfos = jsonObject.getJsonArray("content_info").stream().map(JsonValue::toString).map(ContentInfo::fromJson).collect(Collectors.toList());
            }
        }
    }

    @Override
    public String toString() {
        return "TransferTaskOutput{" +
                "contentInfos=" + contentInfos +
                '}';
    }
}
