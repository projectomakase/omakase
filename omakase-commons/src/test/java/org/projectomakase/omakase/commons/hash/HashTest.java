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
package org.projectomakase.omakase.commons.hash;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class HashTest {

    @Test
    public void shouldThrowIllegalArgumentExceptionIfOffsetIsLessThanZero() {
        assertThatThrownBy(() -> new Hash("test", "test", -1, 100)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("the offset can not be less than 0");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfLengthIsLessThanOffset() {
        assertThatThrownBy(() -> new Hash("test", "test", 100, 50)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("the length must be greater than the offset");
    }

    @Test
    public void shouldSerializeToJson() {
        String expected = "{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}";
        assertThat(new Hash("MD5", "123").toJson()).isEqualTo(expected);
    }

    @Test
    public void shouldSerializeToJsonWithOffsetAndLength() {
        String expected = "{\"hash_algorithm\":\"MD5\",\"offset\":50,\"length\":100,\"hash\":\"123\"}";
        assertThat(new Hash("MD5", "123", 50, 100).toJson()).isEqualTo(expected);
    }

    @Test
    public void shouldDeserializeJsonToHash() {
        String json = "{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}";
        assertThat(Hash.builder().fromJson(json)).isEqualToComparingFieldByField(new Hash("MD5", "123"));
    }

    @Test
    public void shouldDeserializeJsonToHashWithOffsetAndLength() {
        String json = "{\"hash_algorithm\":\"MD5\",\"offset\":50,\"length\":100,\"hash\":\"123\"}";
        assertThat(Hash.builder().fromJson(json)).isEqualToComparingFieldByField(new Hash("MD5", "123", 50, 100));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfNoAlgorithm() {
        String json = "{\"hash\":\"123\"}";
        assertThatThrownBy(() -> Hash.builder().fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hash_algorithm' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfNoValue() {
        String json = "{\"hash_algorithm\":\"MD5\"}";
        assertThatThrownBy(() -> Hash.builder().fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hash' property");
    }

}