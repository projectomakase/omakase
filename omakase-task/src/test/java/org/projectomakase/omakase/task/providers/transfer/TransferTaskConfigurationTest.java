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
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class TransferTaskConfigurationTest {

    @Test
    public void shouldBuildConfigurationFromJson() throws Exception{
        String json = "{\"io_instructions\":[{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\"}],\"hash_algorithms\":[]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        configuration.fromJson(json);
        assertThat(configuration.getIoInstructions()).usingFieldByFieldElementComparator().contains(new IOInstruction(new URI("file:/test/test.txt"), new URI("file:/dest/test.txt")));
        assertThat(configuration.getHashAlgorithms()).isEmpty();
    }

    @Test
    public void shouldBuildConfigurationFromJsonWithHashAlgorithms() throws Exception {
        String json = "{\"io_instructions\":[{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\"}],\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        configuration.fromJson(json);
        assertThat(configuration.getIoInstructions()).usingFieldByFieldElementComparator().contains(new IOInstruction(new URI("file:/test/test.txt"), new URI("file:/dest/test.txt")));
        assertThat(configuration.getHashAlgorithms()).containsExactly("MD5", "SHA256");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoIoInstructions() {
        String json = "{\"hash_algorithms\":[]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'io_instructions' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoSource() {
        String json = "{\"io_instructions\":[{\"destination\":\"file:/dest/test.txt\"}],\"hash_algorithms\":[]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'source' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionInvalidSource() {
        String json = "{\"io_instructions\":[{\"source\":\"[]file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\"}],\"hash_algorithms\":[]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, 'source' is not a valid URI");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoDestination() {
        String json = "{\"io_instructions\":[{\"source\":\"file:/test/test.txt\"}],\"hash_algorithms\":[]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'destination' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionInvalidDestination() {
        String json = "{\"io_instructions\":[{\"source\":\"file:/test/test.txt\",\"destination\":\"f[]ile:/dest/test.txt\"}],\"hash_algorithms\":[]}";
        TransferTaskConfiguration configuration = new TransferTaskConfiguration();
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, 'destination' is not a valid URI");
    }

    @Test
    public void shouldConvertConfigurationToJson() throws Exception {
        String actual = new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(new URI("file:/test/test.txt"), new URI("file:/dest/test.txt"))), Collections.emptyList()).toJson();
        String expected = "{\"io_instructions\":[{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\"}],\"hash_algorithms\":[]}";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConvertConfigurationToJsonWithHashAlgorithms() throws Exception{
        String actual = new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(new URI("file:/test/test.txt"), new URI("file:/dest/test.txt"))), Arrays.asList("MD5", "SHA256")).toJson();
        String expected = "{\"io_instructions\":[{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\"}],\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        assertThat(actual).isEqualTo(expected);
    }
}