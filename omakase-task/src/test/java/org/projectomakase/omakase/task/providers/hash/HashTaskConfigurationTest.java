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

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class HashTaskConfigurationTest {

    @Test
    public void shouldBuildConfigurationFromJson() {
        String json = "{\"source\":\"file:/test/test.txt\",\"hashes\":[{\"hash_algorithm\":\"MD5\"}]}";
        HashTaskConfiguration configuration = new HashTaskConfiguration();
        configuration.fromJson(json);
        assertThat(configuration.getSource().toString()).isEqualTo("file:/test/test.txt");
        assertThat(configuration.getHashes()).hasSize(1);
        assertThat(configuration.getHashes().get(0)).isEqualToComparingFieldByField(new HashInput("MD5"));
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoSource() {
        String json = "{\"hashes\":[{\"hash_algorithm\":\"MD5\"}]}";
        HashTaskConfiguration configuration = new HashTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'source' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionInvalidSource() {
        String json = "{\"source\":\"[]test/test.txt\",\"hashes\":[{\"hash_algorithm\":\"MD5\"}]}";
        HashTaskConfiguration configuration = new HashTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, 'source' is not a valid URI");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoHashes() {
        String json = "{\"source\":\"file:/test/test.txt\"}";
        HashTaskConfiguration configuration = new HashTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'hashes' property");
    }

    @Test
    public void shouldConvertConfigurationToJson() throws Exception{
        String actual = new HashTaskConfiguration(new URI("file:/test/test.txt"), Collections.singletonList(new HashInput("MD5"))).toJson();
        String expected = "{\"source\":\"file:/test/test.txt\",\"hashes\":[{\"hash_algorithm\":\"MD5\"}]}";
        assertThat(actual).isEqualTo(expected);
    }
}