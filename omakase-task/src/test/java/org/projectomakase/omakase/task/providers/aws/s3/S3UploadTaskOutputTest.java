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
package org.projectomakase.omakase.task.providers.aws.s3;

import org.projectomakase.omakase.commons.aws.s3.S3Part;
import org.projectomakase.omakase.commons.hash.Hash;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class S3UploadTaskOutputTest {

    @Test
    public void shouldBuildOutputFromJson() {
        String json = "{\"parts\":[{\"number\":1,\"etag\":\"abc\"}],\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}";
        S3UploadTaskOutput output = new S3UploadTaskOutput();
        output.fromJson(json);
        assertThat(output.getParts()).usingFieldByFieldElementComparator().contains(new S3Part(1, "abc"));
        assertThat(output.getHashes()).usingFieldByFieldElementComparator().contains(new Hash("MD5", "123"));
    }

    @Test
    public void shouldBuildOutputFromJsonWithOffsetAndLength() {
        String json = "{\"parts\":[{\"number\":1,\"etag\":\"abc\"}],\"hashes\":[{\"hash_algorithm\":\"MD5\",\"offset\":50,\"length\":100,\"hash\":\"123\"}]}";
        S3UploadTaskOutput output = new S3UploadTaskOutput();
        output.fromJson(json);
        assertThat(output.getParts()).usingFieldByFieldElementComparator().contains(new S3Part(1, "abc"));
        assertThat(output.getHashes()).usingFieldByFieldElementComparator().contains(new Hash("MD5", "123", 50, 100));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoParts() {
        String json = "{}";
        S3UploadTaskOutput output = new S3UploadTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'parts' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoHashes() {
        String json = "{\"parts\":[{\"number\":1,\"etag\":\"abc\"}]}";
        S3UploadTaskOutput output = new S3UploadTaskOutput();
        assertThatThrownBy(() -> output.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hashes' property");
    }

    @Test
    public void shouldSerializeToJson() {
        S3UploadTaskOutput transferTaskOutput = new S3UploadTaskOutput(Collections.singletonList(new S3Part(1, "abc")), Collections.singletonList(new Hash("MD5", "123")));
        assertThat(transferTaskOutput.toJson()).isEqualTo("{\"parts\":[{\"number\":1,\"etag\":\"abc\"}],\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}");
    }

}