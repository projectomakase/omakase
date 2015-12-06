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
package org.projectomakase.omakase.task.providers.delete;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class DeleteTaskConfigurationTest {

    @Test
    public void shouldBuildConfigurationFromJSON() throws Exception {
        String json = "{\"locations\":[\"file:/file1.txt\", \"file:/file2.txt\"]}";
        DeleteTaskConfiguration configuration = new DeleteTaskConfiguration();
        configuration.fromJson(json);
        DeleteTaskConfigurationAssert.assertThat(configuration).hasLocations(ImmutableList.of(new URI("file:/file1.txt"), new URI("file:/file2.txt")));
    }

    @Test
    public void shouldConvertConfigurationToJson() throws Exception{
        String actual = new DeleteTaskConfiguration(ImmutableList.of(new URI("file:/file1.txt"), new URI("file:/file2.txt"))).toJson();
        String expected = "{\"locations\":[\"file:/file1.txt\",\"file:/file2.txt\"]}";
        assertThat(actual).isEqualTo(expected);
    }
}