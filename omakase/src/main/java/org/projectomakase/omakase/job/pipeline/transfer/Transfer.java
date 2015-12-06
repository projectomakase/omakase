/*
 * #%L
 * omakase
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
package org.projectomakase.omakase.job.pipeline.transfer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a {@link Transfer}. A Transfer can consist of one or more {@link TransferFileGroup}.
 *
 * @author Richard Lucas
 */
public final class Transfer {

    private static final String TRANSFER_FILE_GROUPS = "transfer_file_groups";

    private final List<TransferFileGroup> transferFileGroups;

    public Transfer(List<TransferFileGroup> transferFileGroups) {
        this.transferFileGroups = transferFileGroups;
    }

    public List<TransferFileGroup> getTransferFileGroups() {
        return transferFileGroups;
    }

    /**
     * Serializes the {@link Transfer} into a JSON representation of the transfer.
     *
     * @return a JSON representation of the transfer.
     */
    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format("\"" + TRANSFER_FILE_GROUPS + "\":[%s]", getTransferFileGroups().stream().map(TransferFileGroup::toJson).collect(Collectors.joining(", "))));
        return "{" + String.join(",", attributes) + "}";
    }

    /**
     * Creates a {@link Transfer} from a JSON representation.
     *
     * @param json
     *         the JSON representation
     * @return a {@link Transfer}
     */
    public static Transfer fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            return new Transfer(jsonObject.getJsonArray(TRANSFER_FILE_GROUPS).stream().map(JsonValue::toString).map(TransferFileGroup::fromJson).collect(Collectors.toList()));
        }
    }
}
