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
package org.projectomakase.omakase.task.providers.aws.glacier;

import org.projectomakase.omakase.commons.hash.Hash;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class GlacierUploadTaskOutputTest {

    @Test
    public void shouldBuildOutputFromJson() {
        String json = "{\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}";
        GlacierUploadTaskOutput output = new GlacierUploadTaskOutput();
        output.fromJson(json);
        assertThat(output.getHashes()).extracting(Hash::getAlgorithm, Hash::getValue).containsExactly(new Tuple("MD5", "123"));
    }

    @Test
    public void shouldBuildOutputFromJsonWithOffsetAndLength() {
        String json = "{\"hashes\":[{\"hash_algorithm\":\"MD5\",\"offset\":50,\"length\":100,\"hash\":\"123\"}]}";
        GlacierUploadTaskOutput output = new GlacierUploadTaskOutput();
        output.fromJson(json);
        assertThat(output.getHashes().get(0)).isEqualToComparingFieldByField(new Hash("MD5", "123", 50, 100));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoHashes() {
        String json = "{}";
        GlacierUploadTaskOutput output = new GlacierUploadTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hashes' property");
    }

    @Test
    public void shouldSerializeToJson() {
        GlacierUploadTaskOutput transferTaskOutput = new GlacierUploadTaskOutput(Collections.singletonList(new Hash("MD5", "123")));
        assertThat(transferTaskOutput.toJson()).isEqualTo("{\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}");
    }

}