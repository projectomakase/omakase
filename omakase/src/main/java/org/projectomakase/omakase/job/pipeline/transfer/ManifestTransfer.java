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

import org.projectomakase.omakase.job.configuration.ManifestType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a {@link ManifestTransfer}. A Manifest Transfer can consist of one or more {@link ManifestTransferFile} instances.
 *
 * @author Richard Lucas
 */
public class ManifestTransfer {

    private static final String ROOT_PATH = "root_path";
    private static final String MANIFEST_TYPE = "manifest_type";
    private static final String MANIFEST_TRANSFER_FILES = "manifest_transfer_files";

    private final Path rootPath;
    private final ManifestType manifestType;
    private final List<ManifestTransferFile> manifestTransferFiles;

    public ManifestTransfer(Path rootPath, ManifestType manifestType, List<ManifestTransferFile> manifestTransferFiles) {
        this.rootPath = rootPath;
        this.manifestType = manifestType;
        this.manifestTransferFiles = manifestTransferFiles;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public ManifestType getManifestType() {
        return manifestType;
    }

    public List<ManifestTransferFile> getManifestTransferFiles() {
        return manifestTransferFiles;
    }

    /**
     * Serializes the {@link ManifestTransfer} into a JSON representation of the manifest transfer.
     *
     * @return a JSON representation of the transfer.
     */
    public String toJson() {
        List<String> attributes = new ArrayList<>();
        attributes.add(String.format("\"%s\":\"%s\"", ROOT_PATH, getRootPath().toString()));
        attributes.add(String.format("\"%s\":\"%s\"", MANIFEST_TYPE, getManifestType().name()));
        attributes.add(String.format("\"%s\":[%s]", MANIFEST_TRANSFER_FILES, getManifestTransferFiles().stream().map(ManifestTransferFile::toJson).collect(Collectors.joining(", "))));
        return "{" + String.join(",", attributes) + "}";
    }

    /**
     * Creates a {@link ManifestTransfer} from a JSON representation.
     *
     * @param json
     *         the JSON representation
     * @return a {@link ManifestTransfer}
     */
    public static ManifestTransfer fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();
            Path path = Paths.get(jsonObject.getString(ROOT_PATH));
            ManifestType manifestType = ManifestType.valueOf(jsonObject.getString(MANIFEST_TYPE));
            return new ManifestTransfer(path, manifestType, jsonObject.getJsonArray(MANIFEST_TRANSFER_FILES).stream().map(JsonValue::toString).map(ManifestTransferFile::fromJson).collect(Collectors.toList()));
        }
    }
}
