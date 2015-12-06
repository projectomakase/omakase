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
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.providers.manifest.Manifest;
import org.projectomakase.omakase.task.providers.manifest.ManifestFile;

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
 * Represents a single manifest file being transferred from a source uri to a destination uri.
 *
 * @author Richard Lucas
 */
public final class ManifestTransferFile {

    private static final String ID = "id";
    private static final String SOURCE = "source";
    private static final String DESTINATION = "destination";
    private static final String REPOSITORY_FILE_ID = "repository_file_id";
    private static final String ORIGINAL_FILE_NAME = "original_file_name";
    private static final String ORIGINAL_FILE_PATH = "original_file_path";
    private static final String DESCRIPTION = "description";
    private static final String SIZE = "size";
    private static final String SOURCE_HASHES = "source_hashes";
    private static final String OUTPUT_HASHES = "output_hashes";
    private static final String CHILD_MANIFESTS = "child_manifests";
    private static final String MANIFEST_FILES = "manifest_files";

    private final String id;
    private final String repositoryFileId;
    private final String originalFilename;
    private final String originalFilepath;
    private final String description;
    private final URI source;
    private final URI destination;
    private final Long size;
    private final List<Hash> sourceHashes;
    private final List<Hash> outputHashes;
    private final List<Manifest> childManifests;
    private final List<ManifestFile> manifestFiles;


    private ManifestTransferFile(String id, String repositoryFileId, String originalFilename, String originalFilepath, String description, URI source, URI destination, Long size,
                                 List<Hash> sourceHashes,
                                 List<Hash> outputHashes, List<Manifest> childManifests, List<ManifestFile> manifestFiles) {
        this.id = id;
        this.repositoryFileId = repositoryFileId;
        this.originalFilename = originalFilename;
        this.originalFilepath = originalFilepath;
        this.description = description;
        this.source = source;
        this.destination = destination;
        this.size = size;
        this.sourceHashes = sourceHashes;
        this.outputHashes = outputHashes;
        this.childManifests = childManifests;
        this.manifestFiles = manifestFiles;
    }

    public String getId() {
        return id;
    }

    public String getRepositoryFileId() {
        return repositoryFileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getOriginalFilepath() {
        return originalFilepath;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
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

    public List<Hash> getSourceHashes() {
        return sourceHashes;
    }

    public List<Hash> getOutputHashes() {
        return outputHashes;
    }

    public List<Manifest> getChildManifests() {
        return childManifests;
    }

    public List<ManifestFile> getManifestFiles() {
        return manifestFiles;
    }

    /**
     * Serializes a {@link ManifestTransferFile} into a JSON representation of the manifest.
     *
     * @return a JSON representation of the manifest.
     */
    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format("\"%s\":\"%s\"", ID, getId()));
        attributes.add(String.format("\"%s\":\"%s\"", REPOSITORY_FILE_ID, getRepositoryFileId()));
        attributes.add(String.format("\"%s\":\"%s\"", ORIGINAL_FILE_NAME, getOriginalFilename()));
        attributes.add(String.format("\"%s\":\"%s\"", ORIGINAL_FILE_PATH, getOriginalFilepath()));
        getDescription().ifPresent(desc -> attributes.add(String.format("\"%s\":\"%s\"", DESCRIPTION, desc)));
        attributes.add(String.format("\"%s\":\"%s\"", SOURCE, getSource().toString()));
        attributes.add(String.format("\"%s\":\"%s\"", DESTINATION, getDestination().toString()));
        getSize().ifPresent(manifestSize -> attributes.add(String.format("\"%s\":%d", SIZE, manifestSize)));
        attributes.add(String.format("\"%s\":[%s]", SOURCE_HASHES, getSourceHashes().stream().map(Hash::toJson).collect(Collectors.joining(", "))));
        attributes.add(String.format("\"%s\":[%s]", OUTPUT_HASHES, getOutputHashes().stream().map(Hash::toJson).collect(Collectors.joining(", "))));
        attributes.add(String.format("\"%s\":[%s]", CHILD_MANIFESTS, getChildManifests().stream().map(Manifest::toJson).collect(Collectors.joining(","))));
        attributes.add(String.format("\"%s\":[%s]", MANIFEST_FILES, getManifestFiles().stream().map(ManifestFile::toJson).collect(Collectors.joining(","))));
        return "{" + String.join(",", attributes) + "}";
    }

    /**
     * Creates a {@link ManifestTransferFile} from a JSON representation.
     *
     * @param json
     *         the JSON representation
     * @return a {@link ManifestTransferFile}
     */
    public static ManifestTransferFile fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            ManifestTransferFile.Builder builder = ManifestTransferFile.builder();
            builder.id(jsonObject.getString(ID))
                    .repositoryFileId(jsonObject.getString(REPOSITORY_FILE_ID))
                    .originalFilename(jsonObject.getString(ORIGINAL_FILE_NAME))
                    .originalFilepath(jsonObject.getString(ORIGINAL_FILE_PATH))
                    .source(getURIProperty(jsonObject, SOURCE))
                    .destination(getURIProperty(jsonObject, DESTINATION))
                    .sourceHashes(jsonObject.getJsonArray(SOURCE_HASHES).stream().map(JsonValue::toString).map(hashJson -> Hash.builder().fromJson(hashJson)).collect(Collectors.toList()))
                    .outputHashes(jsonObject.getJsonArray(OUTPUT_HASHES).stream().map(JsonValue::toString).map(hashJson -> Hash.builder().fromJson(hashJson)).collect(Collectors.toList()))
                    .childManifests(jsonObject.getJsonArray(CHILD_MANIFESTS).stream().map(JsonValue::toString).map(Manifest::fromJson).collect(Collectors.toList()))
                    .manifestFiles(jsonObject.getJsonArray(MANIFEST_FILES).stream().map(JsonValue::toString).map(ManifestFile::fromJson).collect(Collectors.toList()));

            if (jsonObject.containsKey(DESCRIPTION)) {
                builder.description(jsonObject.getString(DESCRIPTION));
            }

            if (jsonObject.containsKey(SIZE)) {
                builder.size(jsonObject.getJsonNumber(SIZE).longValue());
            }

            return builder.build();
        }
    }

    /**
     * Returns a builder that can be used to build a new {@link ManifestTransferFile}.
     *
     * @return a builder that can be used to build a new {@link ManifestTransferFile}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder that can be used a new {@link ManifestTransferFile} from an existing one.
     *
     * @param manifestTransferFile
     *         the existing manifest
     * @return a builder that can be used a new {@link ManifestTransferFile} from an existing one.
     */
    public static Builder builder(ManifestTransferFile manifestTransferFile) {
        return new Builder(manifestTransferFile);
    }

    private static URI getURIProperty(JsonObject jsonObject, String key) {
        return Throwables.returnableInstance(() -> new URI(jsonObject.getString(key)));
    }

    /**
     * {@link ManifestTransferFile} builder.
     */
    public static final class Builder {

        private String id;
        private String repositoryFileId;
        private String originalFilename;
        private String originalFilepath;
        private String description;
        private URI source;
        private URI destination;
        private Long size;
        private List<Hash> sourceHashes = ImmutableList.of();
        private List<Hash> outputHashes = ImmutableList.of();
        private List<Manifest> childManifests = ImmutableList.of();
        private List<ManifestFile> manifestFiles = ImmutableList.of();

        private Builder() {
            id = UUID.randomUUID().toString();
        }

        private Builder(ManifestTransferFile manifestTransferFile) {
            id = manifestTransferFile.getId();
            repositoryFileId = manifestTransferFile.getRepositoryFileId();
            originalFilename = manifestTransferFile.getOriginalFilename();
            originalFilepath = manifestTransferFile.getOriginalFilepath();
            description = manifestTransferFile.getDescription().orElse(null);
            source = manifestTransferFile.getSource();
            destination = manifestTransferFile.getDestination();
            size = manifestTransferFile.getSize().orElse(null);
            sourceHashes = manifestTransferFile.getSourceHashes();
            outputHashes = manifestTransferFile.getOutputHashes();
            childManifests = manifestTransferFile.getChildManifests();
            manifestFiles = manifestTransferFile.getManifestFiles();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder repositoryFileId(String repositoryFileId) {
            this.repositoryFileId = repositoryFileId;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder originalFilepath(String originalFilepath) {
            this.originalFilepath = originalFilepath;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public Builder sourceHashes(List<Hash> sourceHashes) {
            this.sourceHashes = sourceHashes;
            return this;
        }

        public Builder outputHashes(List<Hash> outputHashes) {
            this.outputHashes = outputHashes;
            return this;
        }

        public Builder childManifests(List<Manifest> childManifests) {
            this.childManifests = childManifests;
            return this;
        }

        public Builder manifestFiles(List<ManifestFile> manifestFiles) {
            this.manifestFiles = manifestFiles;
            return this;
        }

        /**
         * Builds a {@link ManifestTransferFile}.
         *
         * @return a new {@link} Transfer.
         */
        public ManifestTransferFile build() {
            return new ManifestTransferFile(id, repositoryFileId, originalFilename, originalFilepath, description, source, destination, size, sourceHashes, outputHashes, childManifests,
                                            manifestFiles);
        }

    }
}
