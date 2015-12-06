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
public class HttpsLocationProviderTest {

    private HttpsLocationProvider httpsLocationProvider;

    @Before
    public void setUp() throws Exception {
        httpsLocationProvider = new HttpsLocationProvider();
    }

    @Test
    public void shouldGetProviderType() throws Exception {
        assertThat(httpsLocationProvider.getType()).isEqualTo("HTTPS");
    }

    @Test
    public void shouldGetConfigurationType() throws Exception {
        assertThat(httpsLocationProvider.getConfigurationType()).isEqualTo(NetLocationConfiguration.class);
    }

    @Test
    public void shouldGetLocationUriWithRequiredFields() throws Exception {
        Location location = new Location("test", "test", "HTTPS");
        location.setLocationConfiguration(new NetLocationConfiguration("test.com", 4343));
        assertThat(httpsLocationProvider.getLocationUri(location)).isEqualTo(new URI("https://test.com:4343"));
    }

    @Test
    public void shouldGetLocationUriWithAllFields() throws Exception {
        Location location = new Location("test", "test", "HTTPS");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 4343);
        configuration.setRoot("/hello");
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(httpsLocationProvider.getLocationUri(location)).isEqualTo(new URI("https://user:password@test.com:4343/hello"));
    }

    @Test
    public void shouldGetLocationUriWithPath() throws Exception {
        Location location = new Location("test", "test", "HTTPS");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 4343);
        configuration.setRoot("/hello");
        location.setLocationConfiguration(configuration);
        assertThat(httpsLocationProvider.getLocationUri(location)).isEqualTo(new URI("https://test.com:4343/hello"));
    }

    @Test
    public void shouldGetLocationUriWithUserInfo() throws Exception {
        Location location = new Location("test", "test", "HTTPS");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 4343);
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(httpsLocationProvider.getLocationUri(location)).isEqualTo(new URI("https://user:password@test.com:4343"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoOne() throws Exception {
        Location location = new Location("test", "test", "HTTPS");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 4343);
        configuration.setUsername("user");
        location.setLocationConfiguration(configuration);
        assertThat(httpsLocationProvider.getLocationUri(location)).isEqualTo(new URI("https://test.com:4343"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoTwo() throws Exception {
        Location location = new Location("test", "test", "HTTPS");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 4343);
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(httpsLocationProvider.getLocationUri(location)).isEqualTo(new URI("https://test.com:4343"));
    }
}