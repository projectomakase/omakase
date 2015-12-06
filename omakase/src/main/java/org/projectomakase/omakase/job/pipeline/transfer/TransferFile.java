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
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hash;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a single file being transferred from a source uri to a destination uri.
 *
 * @author Richard Lucas
 */
public final class TransferFile {

    private static final String ID = "id";
    private static final String SOURCE = "source";
    private static final String DESTINATION = "destination";
    private static final String SOURCE_REPOSITORY_FILE_ID = "source_repository_file_id";
    private static final String DESTINATION_REPOSITORY_FILE_ID = "destination_repository_file_id";
    private static final String ORIGINAL_FILE_NAME = "original_file_name";
    private static final String ORIGINAL_FILE_PATH = "original_file_path";
    private static final String SIZE = "size";
    private static final String PART_SIZE = "part_size";
    private static final String SOURCE_HASHES = "source_hashes";
    private static final String OUTPUT_HASHES = "output_hashes";
    private static final String PARTS = "parts";
    private static final String ARCHIVE_ID = "archive_id";

    private final String id;
    private final String sourceRepositoryFileId;
    private final String destinationRepositoryFileId;
    private final String originalFilename;
    private final String originalFilepath;
    private final URI source;
    private final URI destination;
    private final Long size;
    private final Long partSize;
    private final List<Hash> sourceHashes;
    private final List<Hash> outputHashes;
    private final List<AWSUploadPart> parts;
    private final String archiveId;


    private TransferFile(String id, String sourceRepositoryFileId, String destinationRepositoryFileId, String originalFilename, String originalFilepath, URI source, URI destination, Long size, Long partSize,
                         List<Hash> sourceHashes, List<Hash> outputHashes, List<AWSUploadPart> parts, String archiveId) {
        this.id = id;
        this.sourceRepositoryFileId = sourceRepositoryFileId;
        this.destinationRepositoryFileId = destinationRepositoryFileId;
        this.originalFilename = originalFilename;
        this.originalFilepath = originalFilepath;
        this.source = source;
        this.destination = destination;
        this.size = size;
        this.partSize = partSize;
        this.sourceHashes = sourceHashes;
        this.outputHashes = outputHashes;
        this.parts = parts;
        this.archiveId = archiveId;
    }

    public String getId() {
        return id;
    }

    public String getSourceRepositoryFileId() {
        return sourceRepositoryFileId;
    }

    public Optional<String> getDestinationRepositoryFileId() {
        return Optional.ofNullable(destinationRepositoryFileId);
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getOriginalFilepath() {
        return originalFilepath;
    }

    public URI getSource() {
        return source;
    }

    public URI getDestination() {
        return destination;
    }

    public Optional<Long> getSize() {
        return Optional.ofNullable(size);
    }

    public Optional<Long> getPartSize() {
        return Optional.ofNullable(partSize);
    }

    public List<Hash> getSourceHashes() {
        return sourceHashes;
    }

    public List<Hash> getOutputHashes() {
        return outputHashes;
    }

    public List<AWSUploadPart> getParts() {
        return parts;
    }

    public Optional<String> getArchiveId() {
        return Optional.ofNullable(archiveId);
    }

    /**
     * Serializes the {@link TransferFile} into a JSON representation of the transfer.
     *
     * @return a JSON representation of the transfer.
     */
    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format("\"%s\":\"%s\"", ID, getId()));
        attributes.add(String.format("\"%s\":\"%s\"", SOURCE_REPOSITORY_FILE_ID, getSourceRepositoryFileId()));
        getDestinationRepositoryFileId().ifPresent(repositoryFileId -> attributes.add(String.format("\"%s\":\"%s\"", DESTINATION_REPOSITORY_FILE_ID, repositoryFileId)));
        attributes.add(String.format("\"%s\":\"%s\"", ORIGINAL_FILE_NAME, getOriginalFilename()));
        attributes.add(String.format("\"%s\":\"%s\"", ORIGINAL_FILE_PATH, getOriginalFilepath()));
        attributes.add(String.format("\"%s\":\"%s\"", SOURCE, getSource().toString()));
        attributes.add(String.format("\"%s\":\"%s\"", DESTINATION, getDestination().toString()));
        getSize().ifPresent(fileSize -> attributes.add(String.format("\"%s\":%d", SIZE, fileSize)));
        getPartSize().ifPresent(filePartSize -> attributes.add(String.format("\"%s\":%d", PART_SIZE, filePartSize)));
        attributes.add(String.format("\"%s\":[%s]", SOURCE_HASHES, getSourceHashes().stream().map(Hash::toJson).collect(Collectors.joining(", "))));
        attributes.add(String.format("\"%s\":[%s]", OUTPUT_HASHES, getOutputHashes().stream().map(Hash::toJson).collect(Collectors.joining(", "))));
        attributes.add(String.format("\"%s\":[%s]", PARTS, getParts().stream().map(AWSUploadPart::toJson).collect(Collectors.joining(", "))));
        getArchiveId().ifPresent(fileArchiveId -> attributes.add(String.format("\"%s\":\"%s\"", ARCHIVE_ID, fileArchiveId)));
        return "{" + String.join(",", attributes) + "}";
    }

    /**
     * Creates a {@link TransferFile} from a JSON representation.
     *
     * @param json
     *         the JSON representation
     * @return a {@link TransferFile}
     */
    public static TransferFile fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            Builder builder = builder();
            builder.id(jsonObject.getString(ID)).sourceRepositoryFileId(jsonObject.getString(SOURCE_REPOSITORY_FILE_ID))
                    .originalFilename(jsonObject.getString(ORIGINAL_FILE_NAME))
                    .originalFilepath(jsonObject.getString(ORIGINAL_FILE_PATH)).source(Throwables.returnableInstance(() -> new URI(jsonObject.getString(SOURCE)))).destination(Throwables.returnableInstance(() -> new URI(jsonObject.getString(DESTINATION))))
                    .sourceHashes(jsonObject.getJsonArray(SOURCE_HASHES).stream().map(JsonValue::toString).map(hashJson -> Hash.builder().fromJson(hashJson)).collect(Collectors.toList()))
                    .outputHashes(jsonObject.getJsonArray(OUTPUT_HASHES).stream().map(JsonValue::toString).map(hashJson -> Hash.builder().fromJson(hashJson)).collect(Collectors.toList()))
                    .parts(jsonObject.getJsonArray(PARTS).stream().map(JsonValue::toString).map(partJson -> AWSUploadPart.builder().fromJson(partJson)).collect(Collectors.toList()));

            if (jsonObject.containsKey(DESTINATION_REPOSITORY_FILE_ID)) {
                builder.destinationRepositoryFileId(jsonObject.getString(DESTINATION_REPOSITORY_FILE_ID));
            }

            if (jsonObject.containsKey(SIZE)) {
                builder.size(jsonObject.getJsonNumber(SIZE).longValue());
            }

            if (jsonObject.containsKey(PART_SIZE)) {
                builder.partSize(jsonObject.getJsonNumber(PART_SIZE).longValue());
            }

            if (jsonObject.containsKey(ARCHIVE_ID)) {
                builder.archiveId(jsonObject.getString(ARCHIVE_ID));
            }

            return builder.build();
        }
    }

    /**
     * Returns a builder that can be used to build a new {@link TransferFile}.
     *
     * @return a builder that can be used to build a new {@link TransferFile}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder that can be used a new {@link TransferFile} from an existing one.
     *
     * @param transferFile
     *         the existing transfer
     * @return a builder that can be used a new {@link TransferFile} from an existing one.
     */
    public static Builder builder(TransferFile transferFile) {
        return new Builder(transferFile);
    }

    /**
     * {@link TransferFile} builder.
     */
    public static final class Builder {

        private String id;
        private String sourceRepositoryFileId;
        private String destinationRepositoryFileId;
        private String originalFilename;
        private String originalFilepath;
        private URI source;
        private URI destination;
        private Long size;
        private Long partSize;
        private List<Hash> sourceHashes = ImmutableList.of();
        private List<Hash> outputHashes = ImmutableList.of();
        private List<AWSUploadPart> parts = ImmutableList.of();
        private String archiveId;

        private Builder() {
            id = UUID.randomUUID().toString();
        }

        private Builder(TransferFile transferFile) {
            id = transferFile.getId();
            sourceRepositoryFileId = transferFile.getSourceRepositoryFileId();
            destinationRepositoryFileId = transferFile.getDestinationRepositoryFileId().orElse(null);
            originalFilename = transferFile.getOriginalFilename();
            originalFilepath = transferFile.getOriginalFilepath();
            source = transferFile.getSource();
            destination = transferFile.getDestination();
            size = transferFile.getSize().orElse(null);
            partSize = transferFile.getPartSize().orElse(null);
            sourceHashes = transferFile.getSourceHashes();
            outputHashes = transferFile.getOutputHashes();
            parts = transferFile.getParts();
            archiveId = transferFile.getArchiveId().orElse(null);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sourceRepositoryFileId(String repositoryFileId) {
            this.sourceRepositoryFileId = repositoryFileId;
            return this;
        }

        public Builder destinationRepositoryFileId(String repositoryFileId) {
            this.destinationRepositoryFileId = repositoryFileId;
            return this;
        }

        public Builder originalFilename(String originalFileName) {
            this.originalFilename = originalFileName;
            return this;
        }

        public Builder originalFilepath(String originalFilepath) {
            this.originalFilepath = originalFilepath;
            return this;
        }

        public Builder source(URI source) {
            this.source = source;
            return this;
        }

        public Builder destination(URI destination) {
            this.destination = destination;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder partSize(long partSize) {
            this.partSize = partSize;
            return this;
        }

        public Builder sourceHashes(List<Hash> sourceHashes) {
            this.sourceHashes = sourceHashes;
            return this;
        }

        public Builder outputHashes(List<Hash> outputHashes) {
            this.outputHashes = outputHashes;
            return this;
        }

        public Builder parts(List<AWSUploadPart> parts) {
            this.parts = parts;
            return this;
        }

        public Builder archiveId(String archiveId) {
            this.archiveId = archiveId;
            return this;
        }

        /**
         * Builds a {@link TransferFile}.
         *
         * @return a new {@link TransferFile}.
         */
        public TransferFile build() {
            return new TransferFile(id, sourceRepositoryFileId, destinationRepositoryFileId, originalFilename, originalFilepath, source, destination, size, partSize, sourceHashes, outputHashes, parts, archiveId);
        }

    }
}
