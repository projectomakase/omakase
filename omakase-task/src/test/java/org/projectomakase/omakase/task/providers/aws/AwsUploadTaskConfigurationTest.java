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
package org.projectomakase.omakase.task.providers.aws;

import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskConfiguration;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public abstract class AwsUploadTaskConfigurationTest {

    private AWSUploadTaskConfiguration configuration;

    public AwsUploadTaskConfigurationTest(AWSUploadTaskConfiguration configuration) {
        this.configuration = configuration;
    }

    @Test
    public void shouldBuildConfigurationFromJson() {
        String json = "{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\",\"part_size\":1024," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        configuration.fromJson(json);
        assertThat(configuration.getSource().toString()).isEqualTo("file:/test/test.txt");
        assertThat(configuration.getDestination().toString()).isEqualTo("file:/dest/test.txt");
        assertThat(configuration.getParts().get(0)).isEqualToComparingFieldByField(new AWSUploadPart(0, 0, 1023, "123", "abc"));
        assertThat(configuration.getHashAlgorithms()).containsExactly("MD5", "SHA256");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoSource() {
        String json = "{\"destination\":\"file:/dest/test.txt\",\"part_size\":1024," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";

        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'source' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionInvalidSource() {
        String json = "{\"source\":\"[]file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\",\"part_size\":1024," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, 'source' is not a valid URI");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoDestination() {
        String json = "{\"source\":\"file:/test/test.txt\",\"part_size\":1024," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'destination' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionInvalidDestination() {
        String json = "{\"source\":\"file:/test/test.txt\",\"destination\":\"[]file:/dest/test.txt\",\"part_size\":1024," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, 'destination' is not a valid URI");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoPartSize() {
        String json = "{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\"," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'part_size' property");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionNoParts() {
        String json = "{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\",\"part_size\":1024,\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        
        assertThatThrownBy(() -> configuration.fromJson(json)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Invalid JSON, requires a 'parts' property");
    }

    @Test
    public void shouldConvertConfigurationToJson() throws Exception {
        String actual =
                new GlacierUploadTaskConfiguration(new URI("file:/test/test.txt"), new URI("file:/dest/test.txt"), 1024, Collections.singletonList(new AWSUploadPart(0, 0, 1023, "123", "abc")),
                        Arrays.asList("MD5", "SHA256")).toJson();
        String expected = "{\"source\":\"file:/test/test.txt\",\"destination\":\"file:/dest/test.txt\",\"part_size\":1024," +
                "\"parts\":[{\"number\":0,\"offset\":0,\"length\":1023,\"signing_hash\":\"123\",\"part_hash\":\"abc\"}]," +
                "\"hash_algorithms\":[\"MD5\",\"SHA256\"]}";
        assertThat(actual).isEqualTo(expected);
    }
}