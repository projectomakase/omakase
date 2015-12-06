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

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.hash.Hash;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class ManifestTransferTaskOutputTest {

    @Test
    public void shouldBuildOutputFromJson() {
        String json = "{\"manifests\":[{\"uri\":\"test.txt\",\"description\":\"test\"}],\"files\":[],\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}";
        ManifestTransferTaskOutput output = new ManifestTransferTaskOutput();
        output.fromJson(json);
        assertThat(output.getManifests()).hasSize(1);
        assertThat(output.getFiles()).isEmpty();
        assertThat(output.getSize()).isEqualTo(1024);
        Assertions.assertThat(output.getHashes()).extracting(Hash::getAlgorithm, Hash::getValue).containsExactly(new Tuple("MD5", "123"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoManifests() {
        String json = "{\"files\":[],\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}";
        ManifestTransferTaskOutput output = new ManifestTransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'manifests' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoFiles() {
        String json = "{\"manifests\":[],\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}";
        ManifestTransferTaskOutput output = new ManifestTransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'files' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoSize() {
        String json = "{\"manifests\":[{\"uri\":\"test.txt\",\"description\":\"test\"}],\"files\":[],\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}";
        ManifestTransferTaskOutput output = new ManifestTransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'size' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoHashes() {
        String json = "{\"manifests\":[{\"uri\":\"test.txt\",\"description\":\"test\"}],\"files\":[],\"size\":1024}";
        ManifestTransferTaskOutput output = new ManifestTransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hashes' property");
    }

    @Test
    public void shouldSerializeToJson() throws Exception {
        ManifestTransferTaskOutput transferTaskOutput =
                new ManifestTransferTaskOutput(ImmutableList.of(new Manifest(new URI("test.txt"), "test")), ImmutableList.of(new ManifestFile(new URI("test.txt"), 100)), 1024, Collections.singletonList(new Hash("MD5", "123")));
        assertThat(transferTaskOutput.toJson()).isEqualTo("{\"manifests\":[{\"uri\":\"test.txt\",\"description\":\"test\"}],\"files\":[{\"uri\":\"test.txt\",\"size\":100}],\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}],\"size\":1024}");
    }
}