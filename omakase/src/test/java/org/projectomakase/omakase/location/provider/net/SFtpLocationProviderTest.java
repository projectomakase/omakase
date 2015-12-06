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
package org.projectomakase.omakase.location.provider.net;

import org.projectomakase.omakase.location.api.Location;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class SFtpLocationProviderTest {

    private SFtpLocationProvider sFtpLocationProvider;

    @Before
    public void setUp() throws Exception {
        sFtpLocationProvider = new SFtpLocationProvider();
    }

    @Test
    public void shouldGetProviderType() throws Exception {
        assertThat(sFtpLocationProvider.getType()).isEqualTo("SFTP");
    }

    @Test
    public void shouldGetConfigurationType() throws Exception {
        assertThat(sFtpLocationProvider.getConfigurationType()).isEqualTo(NetLocationConfiguration.class);
    }

    @Test
    public void shouldGetLocationUriWithRequiredFields() throws Exception {
        Location location = new Location("test", "test", "SFTP");
        location.setLocationConfiguration(new NetLocationConfiguration("test.com", 22));
        assertThat(sFtpLocationProvider.getLocationUri(location)).isEqualTo(new URI("sftp://test.com:22"));
    }

    @Test
    public void shouldGetLocationUriWithAllFields() throws Exception {
        Location location = new Location("test", "test", "SFTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 22);
        configuration.setRoot("/hello");
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(sFtpLocationProvider.getLocationUri(location)).isEqualTo(new URI("sftp://user:password@test.com:22/hello"));
    }

    @Test
    public void shouldGetLocationUriWithPath() throws Exception {
        Location location = new Location("test", "test", "SFTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 22);
        configuration.setRoot("/hello");
        location.setLocationConfiguration(configuration);
        assertThat(sFtpLocationProvider.getLocationUri(location)).isEqualTo(new URI("sftp://test.com:22/hello"));
    }

    @Test
    public void shouldGetLocationUriWithUserInfo() throws Exception {
        Location location = new Location("test", "test", "SFTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 22);
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(sFtpLocationProvider.getLocationUri(location)).isEqualTo(new URI("sftp://user:password@test.com:22"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoOne() throws Exception {
        Location location = new Location("test", "test", "SFTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 22);
        configuration.setUsername("user");
        location.setLocationConfiguration(configuration);
        assertThat(sFtpLocationProvider.getLocationUri(location)).isEqualTo(new URI("sftp://test.com:22"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoTwo() throws Exception {
        Location location = new Location("test", "test", "SFTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 22);
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(sFtpLocationProvider.getLocationUri(location)).isEqualTo(new URI("sftp://test.com:22"));
    }
}