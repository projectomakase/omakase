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
package org.projectomakase.omakase.task.providers.hash;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class HashInputTest {

    @Test
    public void shouldReturnJsonFromInputWithAlgorithm() {
        assertThat(new HashInput("MD5").toJson()).isEqualTo("{\"hash_algorithm\":\"MD5\"}");
    }

    @Test
    public void shouldReturnJsonFromInputWithAlgorithmAndOffset() {
        assertThat(new HashInput("MD5", 100).toJson()).isEqualTo("{\"hash_algorithm\":\"MD5\",\"offset\":100}");
    }

    @Test
    public void shouldReturnJsonFromInputWithAlgorithmOffsetAndLength() {
        assertThat(new HashInput("MD5", 100, 500).toJson()).isEqualTo("{\"hash_algorithm\":\"MD5\",\"offset\":100,\"length\":500}");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfOffsetIsLessThanZero() {
        assertThatThrownBy(() -> new HashInput("test", -1, 100)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("the offset can not be less than 0");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfLengthIsLessThanOffset() {
        assertThatThrownBy(() -> new HashInput("test", 100, 50)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("the length must be greater than the offset");
    }

    @Test
    public void shouldDeserializeJsonToHashInput() {
        String json = "{\"hash_algorithm\":\"MD5\"}";
        assertThat(HashInput.builder().fromJson(json)).isEqualToComparingFieldByField(new HashInput("MD5"));
    }

    @Test
    public void shouldDeserializeJsonToHashInputWithOffset() {
        String json = "{\"hash_algorithm\":\"MD5\",\"offset\":50}";
        assertThat(HashInput.builder().fromJson(json)).isEqualToComparingFieldByField(new HashInput("MD5", 50));
    }

    @Test
    public void shouldDeserializeJsonToHashInputWithOffsetAndLength() {
        String json = "{\"hash_algorithm\":\"MD5\",\"offset\":50,\"length\":100}";
        assertThat(HashInput.builder().fromJson(json)).isEqualToComparingFieldByField(new HashInput("MD5", 50, 100));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfNoAlgorithm() {
        String json = "{}";
        assertThatThrownBy(() -> HashInput.builder().fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hash_algorithm' property");
    }

}