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
package org.projectomakase.omakase.task.providers.manifest;

import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.spi.TaskOutput;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Richard Lucas
 */
public class ManifestTransferTaskOutput implements TaskOutput {

    private static final String TASK_TYPE = "MANIFEST_TRANSFER";

    private static final String JSON_TEMPLATE = "{\"manifests\":[%s],\"files\":[%s],\"hashes\":[%s],\"size\":%d}";

    private List<Manifest> manifests;
    private List<ManifestFile> files;
    private long size;
    private List<Hash> hashes;

    public ManifestTransferTaskOutput() {
        // required by service loader
    }

    public ManifestTransferTaskOutput(List<Manifest> manifests, List<ManifestFile> files, long size, List<Hash> hashes) {
        this.manifests = manifests;
        this.files = files;
        this.size = size;
        this.hashes = hashes;
    }

    public List<Manifest> getManifests() {
        return manifests;
    }

    public List<ManifestFile> getFiles() {
        return files;
    }

    public long getSize() {
        return size;
    }

    public List<Hash> getHashes() {
        return hashes;
    }


    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toJson() {
        return String.format(JSON_TEMPLATE, manifests.stream().map(Manifest::toJson).collect(Collectors.joining(",")), files.stream().map(ManifestFile::toJson).collect(Collectors.joining(",")),
                             hashes.stream().map(Hash::toJson).collect(Collectors.joining(", ")), size);
    }

    @Override
    public void fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            if (!jsonObject.containsKey("manifests")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'manifests' property");
            } else {
                manifests = jsonObject.getJsonArray("manifests").stream().map(JsonValue::toString).map(Manifest::fromJson).collect(Collectors.toList());
            }

            if (!jsonObject.containsKey("files")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'files' property");
            } else {
                files = jsonObject.getJsonArray("files").stream().map(JsonValue::toString).map(ManifestFile::fromJson).collect(Collectors.toList());
            }

            if (!jsonObject.containsKey("size")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'size' property");
            } else {
                size = jsonObject.getInt("size");
            }

            if (!jsonObject.containsKey("hashes")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'hashes' property");
            } else {
                hashes = jsonObject.getJsonArray("hashes").stream().map(JsonValue::toString).map(Hash.builder()::fromJson).collect(Collectors.toList());
            }
        }
    }

    @Override
    public String toString() {
        return "ManifestTransferTaskOutput{" +
                "manifests=" + manifests +
                ", files=" + files +
                ", size=" + size +
                ", hashes=" + hashes +
                '}';
    }

    private String uriListToString(List<URI> uris) {
        return uris.stream().map(URI::toString).map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
    }
}
