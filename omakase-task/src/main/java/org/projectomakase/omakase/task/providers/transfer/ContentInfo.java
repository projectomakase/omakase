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

import org.projectomakase.omakase.commons.hash.Hash;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Richard Lucas
 */
public class ContentInfo {

    private static final String JSON_TEMPLATE = "{\"source\":\"%s\",\"size\":%d,\"hashes\":[%s]}";

    private final URI source;
    private final long size;
    private final List<Hash> hashes;

    public ContentInfo(URI source, long size, List<Hash> hashes) {
        this.source = source;
        this.size = size;
        this.hashes = hashes;
    }

    public URI getSource() {
        return source;
    }

    public long getSize() {
        return size;
    }

    public List<Hash> getHashes() {
        return hashes;
    }

    public String toJson() {
        return String.format(JSON_TEMPLATE, source.toString(), size, hashes.stream().map(Hash::toJson).collect(Collectors.joining(", ")));
    }

    public static ContentInfo fromJson(String json) {
        try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
            JsonObject jsonObject = jsonReader.readObject();

            long contentSize;
            List<Hash> contentHashes;

            if (!jsonObject.containsKey("size")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'size' property");
            } else {
                contentSize = jsonObject.getInt("size");
            }

            if (!jsonObject.containsKey("hashes")) {
                throw new IllegalArgumentException("Invalid JSON, requires a 'hashes' property");
            } else {
                contentHashes = jsonObject.getJsonArray("hashes").stream().map(JsonValue::toString).map(Hash.builder()::fromJson).collect(Collectors.toList());
            }

            return new ContentInfo(getURIProperty(jsonObject,"source"), contentSize, contentHashes);

        }
    }

    @Override
    public String toString() {
        return "ContentInfo{" +
                "source=" + source +
                ", size=" + size +
                ", hashes=" + hashes +
                '}';
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
}
