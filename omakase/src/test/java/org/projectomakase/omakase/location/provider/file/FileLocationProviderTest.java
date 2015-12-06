/*
 * #%L
 * omakase
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
package org.projectomakase.omakase.location.provider.file;

import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.api.LocationConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class FileLocationProviderTest {

    private FileLocationProvider fileLocationProvider;

    @Before
    public void setUp() throws Exception {
        fileLocationProvider = new FileLocationProvider();
    }

    @Test
    public void shouldGetProviderType() throws Exception {
        assertThat(fileLocationProvider.getType()).isEqualTo("FILE");
    }

    @Test
    public void shouldGetConfigurationType() throws Exception {
        assertThat(fileLocationProvider.getConfigurationType()).isEqualTo(FileLocationConfiguration.class);
    }

    @Test
    public void shouldThrowInvalidRootPathWhenValidatingConfiguration() throws Exception {
        FileLocationConfiguration configuration = new FileLocationConfiguration("test");
        assertThatThrownBy(() -> fileLocationProvider.validateConfiguration(configuration)).isExactlyInstanceOf(LocationConfigurationException.class).hasMessage("root path does not start with /");
    }

    @Test
    public void shouldGetLocationUri() throws Exception {
        Location location = new Location("test", "test", "FILE");
        location.setLocationConfiguration(new FileLocationConfiguration("/test"));
        assertThat(fileLocationProvider.getLocationUri(location)).isEqualTo(new URI("file:/test"));
    }
}