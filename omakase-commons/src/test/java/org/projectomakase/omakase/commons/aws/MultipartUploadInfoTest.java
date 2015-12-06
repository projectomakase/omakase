/*
 * #%L
 * omakase-commons
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
package org.projectomakase.omakase.commons.aws;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class MultipartUploadInfoTest {

    @Test
    public void shouldFailToCreateZeroPartSize() throws Exception {
        assertThatThrownBy(() -> new MultipartUploadInfo(0, ImmutableList.of("A", "B"))).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("partSize must be greater than 0");
    }

    @Test
    public void shouldFailToCreateNegativePartSize() throws Exception {
        assertThatThrownBy(() -> new MultipartUploadInfo(-1, ImmutableList.of("A", "B"))).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("partSize must be greater than 0");
    }

    @Test
    public void shouldFailToCreateNullAlgorithms() throws Exception {
        assertThatThrownBy(() -> new MultipartUploadInfo(1, null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("requiredHashAlgorithms must not be null");
    }

    @Test
    public void shouldFailToCreateNEmptyAlgorithms() throws Exception {
        assertThatThrownBy(() -> new MultipartUploadInfo(1, ImmutableList.of())).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("requiredHashAlgorithms must not be empty");
    }

    @Test
    public void shouldSerializeToJson() throws Exception {
        MultipartUploadInfo multipartUploadInfo = new MultipartUploadInfo(1, ImmutableList.of("A", "B"));
        assertThat(multipartUploadInfo.toJson()).isEqualTo("{\"part_size\":1,\"required_hash_algorithms\":[\"A\",\"B\"]}");
    }

    @Test
    public void shouldCreateFromJson() throws Exception {
        MultipartUploadInfo multipartUploadInfo = MultipartUploadInfo.fromJson("{\"part_size\":1,\"required_hash_algorithms\":[\"A\",\"B\"]}");
        assertThat(multipartUploadInfo.getPartSize()).isEqualTo(1);
        assertThat(multipartUploadInfo.getRequiredHashAlgorithms()).containsExactly("A", "B");
    }

    @Test
    public void shouldFailToCreateFromJsonNoPartSize() throws Exception {
        assertThatThrownBy(() -> MultipartUploadInfo.fromJson("{\"required_hash_algorithms\":[\"A\",\"B\"]}")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(
                "Invalid JSON, requires a 'part_size' property");
    }

    @Test
    public void shouldFailToCreateFromJsonNoAlgorithms() throws Exception {
        assertThatThrownBy(() -> MultipartUploadInfo.fromJson("{\"part_size\":1}")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(
                "Invalid JSON, requires a 'required_hash_algorithms' property");
    }
}