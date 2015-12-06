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

import com.google.common.collect.ImmutableList;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A group of transfer files.
 *
 * @author Richard Lucas
 */
public final class TransferFileGroup {

    private static final String ID = "id";
    private static final String TRANSFER_FILES = "transfer_files";
    private static final String DESCRIPTION = "description";

    private final String id;
    private final List<TransferFile> transferFiles;
    private final String description;

    private TransferFileGroup(String id, List<TransferFile> transferFiles, String description) {
        this.id = id;
        this.transferFiles = transferFiles;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public List<TransferFile> getTransferFiles() {
        return transferFiles;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Serializes the {@link TransferFileGroup} into a JSON representation of the transfer group.
     *
     * @return a JSON representation of the transfer group.
     */
    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format("\"%s\":\"%s\"", ID, getId()));
        getDescription().ifPresent(desc -> attributes.add(String.format("\"%s\":\"%s\"", DESCRIPTION, desc)));
        attributes.add(String.format("\"%s\":[%s]", TRANSFER_FILES, getTransferFiles().stream().map(TransferFile::toJson).collect(Collectors.joining(", "))));
        return "{" + String.join(",", attributes) + "}";
    }

    /**
     * Creates a {@link TransferFileGroup} from a JSON representation.
     *
     * @param json
     *         the JSON representation
     * @return a {@link TransferFileGroup}
     */
    public static TransferFileGroup fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            String description = null;
            if (jsonObject.containsKey(DESCRIPTION)) {
                description = jsonObject.getString(DESCRIPTION);
            }
            return new TransferFileGroup(jsonObject.getString(ID), jsonObject.getJsonArray(TRANSFER_FILES).stream()
                    .map(JsonValue::toString)
                    .map(TransferFile::fromJson)
                    .collect(Collectors.toList()), description);
        }
    }

    /**
     * Returns a builder that can be used to build a new {@link TransferFileGroup}.
     *
     * @return a builder that can be used to build a new {@link TransferFileGroup}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder that can be used a new {@link TransferFileGroup} from an existing one.
     *
     * @param transferFileGroup
     *         the existing transfer
     * @return a builder that can be used a new {@link TransferFileGroup} from an existing one.
     */
    public static Builder builder(TransferFileGroup transferFileGroup) {
        return new Builder(transferFileGroup);
    }

    /**
     * {@link TransferFile} builder.
     */
    public static final class Builder {

        private String id;
        private List<TransferFile> transferFiles = ImmutableList.of();
        private String description = null;

        private Builder() {
            id = UUID.randomUUID().toString();
        }

        private Builder(TransferFileGroup transferFileGroup) {
            id = transferFileGroup.getId();
            transferFiles = transferFileGroup.getTransferFiles();
            description = transferFileGroup.getDescription().orElse(null);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder transferFiles(List<TransferFile> transferFiles) {
            this.transferFiles = transferFiles;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds a {@link TransferFileGroup}.
         *
         * @return a new {@link TransferFileGroup}.
         */
        public TransferFileGroup build() {
            return new TransferFileGroup(id, transferFiles, description);
        }
    }
}
