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
public class TransferTaskOutputTest {

    @Test
    public void shouldBuildOutputFromJson() {
        String json = "{\"content_info\":[{\"source\":\"file:/test.txt\",\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}";
        TransferTaskOutput output = new TransferTaskOutput();
        output.fromJson(json);
        Assertions.assertThat(output.getContentInfos()).hasSize(1);
        assertThat(output.getContentInfos().get(0).getSize()).isEqualTo(1024);
        assertThat(output.getContentInfos().get(0).getHashes()).extracting(Hash::getAlgorithm, Hash::getValue).containsExactly(new Tuple("MD5", "123"));
    }

    @Test
    public void shouldBuildOutputFromJsonWithOffsetAndLength() {
        String json = "{\"content_info\":[{\"source\":\"file:/test.txt\",\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"offset\":50,\"length\":100,\"hash\":\"123\"}]}]}";
        TransferTaskOutput output = new TransferTaskOutput();
        output.fromJson(json);
        Assertions.assertThat(output.getContentInfos()).hasSize(1);
        assertThat(output.getContentInfos().get(0).getSize()).isEqualTo(1024);
        assertThat(output.getContentInfos().get(0).getHashes().get(0)).isEqualToComparingFieldByField(new Hash("MD5", "123", 50, 100));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoContentInfo() {
        String json = "{}";
        TransferTaskOutput output = new TransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'content_info' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoSource() {
        String json = "{\"content_info\":[{\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}";
        TransferTaskOutput output = new TransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'source' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionInvalidSource() {
        String json = "{\"content_info\":[{\"source\":\"[]file:/test.txt\",\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}";
        TransferTaskOutput output = new TransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, 'source' is not a valid URI");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoSize() {
        String json = "{\"content_info\":[{\"source\":\"file:/test.txt\",\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}";
        TransferTaskOutput output = new TransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'size' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoHashes() {
        String json = "{\"content_info\":[{\"source\":\"file:/test.txt\",\"size\":1024}]}";
        TransferTaskOutput output = new TransferTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hashes' property");
    }

    @Test
    public void shouldSerializeToJson() throws Exception {
        TransferTaskOutput transferTaskOutput = new TransferTaskOutput(ImmutableList.of(new ContentInfo(new URI("file:/test.txt"), 1024, Collections.singletonList(new Hash("MD5", "123")))));
        assertThat(transferTaskOutput.toJson()).isEqualTo("{\"content_info\":[{\"source\":\"file:/test.txt\",\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}");
    }
}