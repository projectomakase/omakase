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
public class FtpLocationProviderTest {

    private FtpLocationProvider ftpLocationProvider;

    @Before
    public void setUp() throws Exception {
        ftpLocationProvider = new FtpLocationProvider();
    }

    @Test
    public void shouldGetProviderType() throws Exception {
        assertThat(ftpLocationProvider.getType()).isEqualTo("FTP");
    }

    @Test
    public void shouldGetConfigurationType() throws Exception {
        assertThat(ftpLocationProvider.getConfigurationType()).isEqualTo(FtpLocationConfiguration.class);
    }

    @Test
    public void shouldGetLocationUriWithRequiredFields() throws Exception {
        Location location = new Location("test", "test", "FTP");
        location.setLocationConfiguration(new FtpLocationConfiguration("test.com", 21));
        assertThat(ftpLocationProvider.getLocationUri(location)).isEqualTo(new URI("ftp://test.com:21"));
    }

    @Test
    public void shouldGetLocationUriWithAllFields() throws Exception {
        Location location = new Location("test", "test", "FTP");
        FtpLocationConfiguration configuration = new FtpLocationConfiguration("test.com", 21);
        configuration.setRoot("/hello");
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(ftpLocationProvider.getLocationUri(location)).isEqualTo(new URI("ftp://user:password@test.com:21/hello"));
    }

    @Test
    public void shouldGetLocationUriWithPath() throws Exception {
        Location location = new Location("test", "test", "FTP");
        FtpLocationConfiguration configuration = new FtpLocationConfiguration("test.com", 21);
        configuration.setRoot("/hello");
        location.setLocationConfiguration(configuration);
        assertThat(ftpLocationProvider.getLocationUri(location)).isEqualTo(new URI("ftp://test.com:21/hello"));
    }

    @Test
    public void shouldGetLocationUriWithUserInfo() throws Exception {
        Location location = new Location("test", "test", "FTP");
        FtpLocationConfiguration configuration = new FtpLocationConfiguration("test.com", 21);
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(ftpLocationProvider.getLocationUri(location)).isEqualTo(new URI("ftp://user:password@test.com:21"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoOne() throws Exception {
        Location location = new Location("test", "test", "FTP");
        FtpLocationConfiguration configuration = new FtpLocationConfiguration("test.com", 21);
        configuration.setUsername("user");
        location.setLocationConfiguration(configuration);
        assertThat(ftpLocationProvider.getLocationUri(location)).isEqualTo(new URI("ftp://test.com:21"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoTwo() throws Exception {
        Location location = new Location("test", "test", "FTP");
        FtpLocationConfiguration configuration = new FtpLocationConfiguration("test.com", 21);
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(ftpLocationProvider.getLocationUri(location)).isEqualTo(new URI("ftp://test.com:21"));
    }
}