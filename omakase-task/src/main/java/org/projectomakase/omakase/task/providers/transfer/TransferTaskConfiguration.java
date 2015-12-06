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

import org.projectomakase.omakase.task.spi.TaskConfiguration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transfer Task Configuration.
 *
 * @author Richard Lucas
 */
public class TransferTaskConfiguration implements TaskConfiguration {

    private static final String TASK_TYPE = "TRANSFER";
    private static final String JSON_TEMPLATE = "{\"io_instructions\":[%s],\"hash_algorithms\":[%s]}";

    private List<IOInstruction> ioInstructions;
    private List<String> hashAlgorithms;

    public TransferTaskConfiguration() {
        // required by service loader
    }

    public TransferTaskConfiguration(List<IOInstruction> ioInstructions, List<String> hashAlgorithms) {
        this.ioInstructions = ioInstructions;
        this.hashAlgorithms = hashAlgorithms;
    }

    public List<IOInstruction> getIoInstructions() {
        return ioInstructions;
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
        return String.format(JSON_TEMPLATE, ioInstructions.stream().map(io -> io.toJson()).collect(Collectors.joining(",")), hashAlgorithms.stream().map(algorithm -> "\"" + algorithm + "\"").collect(Collectors.joining(",")));
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("io_instructions")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'io_instructions' property");
            } else {
                ioInstructions = jsonObject.getJsonArray("io_instructions").stream().map(JsonValue::toString).map(IOInstruction::fromJson).collect(Collectors.toList());
            }

            if (jsonObject.containsKey("hash_algorithms")) {
                hashAlgorithms = jsonObject.getJsonArray("hash_algorithms").stream().map(value -> ((JsonString) value).getString()).collect(Collectors.toList());
            } else {
                hashAlgorithms = Collections.emptyList();
            }
        }
    }

    @Override
    public String toString() {
        return "TransferTaskConfiguration{" +
                "ioInstructions=" + ioInstructions +
                ", hashAlgorithms=" + hashAlgorithms +
                '}';
    }
}
