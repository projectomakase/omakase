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
package org.projectomakase.omakase.location.provider.s3;

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
public class S3LocationProviderTest {

    private S3LocationProvider s3LocationProvider;

    @Before
    public void setUp() throws Exception {
        s3LocationProvider = new S3LocationProvider();
    }

    @Test
    public void shouldGetProviderType() throws Exception {
        assertThat(s3LocationProvider.getType()).isEqualTo("S3");
    }

    @Test
    public void shouldGetConfigurationType() throws Exception {
        assertThat(s3LocationProvider.getConfigurationType()).isEqualTo(S3LocationConfiguration.class);
    }

    @Test
    public void shouldGetLocationUriWithRequiredFields() throws Exception {
        Location location = new Location("test", "test", "S3");
        location.setLocationConfiguration(new S3LocationConfiguration("access", "secret", "us-west-1", "test", null));
        assertThat(s3LocationProvider.getLocationUri(location)).isEqualTo(new URI("s3://access:secret@test.s3-us-west-1.amazonaws.com"));
    }

    @Test
    public void shouldGetLocationUriWithPath() throws Exception {
        Location location = new Location("test", "test", "S3");
        location.setLocationConfiguration(new S3LocationConfiguration("access", "secret", "us-west-1", "test", "/a1"));
        assertThat(s3LocationProvider.getLocationUri(location)).isEqualTo(new URI("s3://access:secret@test.s3-us-west-1.amazonaws.com/a1"));
    }

    @Test
    public void shouldThrowExceptionIfSecretKeyContainsForwardSlash() throws Exception {
        assertThatThrownBy(() -> s3LocationProvider.validateConfiguration(new S3LocationConfiguration("access", "secret/", "us-west-1", "test", null))).isInstanceOf(LocationConfigurationException.class)
                .hasMessage("AWS secret keys that contain a '/' are not supported, please use a different key");
    }

}