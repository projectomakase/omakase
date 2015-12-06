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
public class HttpLocationProviderTest {

    private HttpLocationProvider httpLocationProvider;

    @Before
    public void setUp() throws Exception {
        httpLocationProvider = new HttpLocationProvider();
    }

    @Test
    public void shouldGetProviderType() throws Exception {
        assertThat(httpLocationProvider.getType()).isEqualTo("HTTP");
    }

    @Test
    public void shouldGetConfigurationType() throws Exception {
        assertThat(httpLocationProvider.getConfigurationType()).isEqualTo(NetLocationConfiguration.class);
    }

    @Test
    public void shouldGetLocationUriWithRequiredFields() throws Exception {
        Location location = new Location("test", "test", "HTTP");
        location.setLocationConfiguration(new NetLocationConfiguration("test.com", 8080));
        assertThat(httpLocationProvider.getLocationUri(location)).isEqualTo(new URI("http://test.com:8080"));
    }

    @Test
    public void shouldGetLocationUriWithAllFields() throws Exception {
        Location location = new Location("test", "test", "HTTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 8080);
        configuration.setRoot("/hello");
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(httpLocationProvider.getLocationUri(location)).isEqualTo(new URI("http://user:password@test.com:8080/hello"));
    }

    @Test
    public void shouldGetLocationUriWithPath() throws Exception {
        Location location = new Location("test", "test", "HTTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 8080);
        configuration.setRoot("/hello");
        location.setLocationConfiguration(configuration);
        assertThat(httpLocationProvider.getLocationUri(location)).isEqualTo(new URI("http://test.com:8080/hello"));
    }

    @Test
    public void shouldGetLocationUriWithUserInfo() throws Exception {
        Location location = new Location("test", "test", "HTTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 8080);
        configuration.setUsername("user");
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(httpLocationProvider.getLocationUri(location)).isEqualTo(new URI("http://user:password@test.com:8080"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoOne() throws Exception {
        Location location = new Location("test", "test", "HTTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 8080);
        configuration.setUsername("user");
        location.setLocationConfiguration(configuration);
        assertThat(httpLocationProvider.getLocationUri(location)).isEqualTo(new URI("http://test.com:8080"));
    }

    @Test
    public void shouldGetLocationUriWithIncompleteUserInfoTwo() throws Exception {
        Location location = new Location("test", "test", "HTTP");
        NetLocationConfiguration configuration = new NetLocationConfiguration("test.com", 8080);
        configuration.setPassword("password");
        location.setLocationConfiguration(configuration);
        assertThat(httpLocationProvider.getLocationUri(location)).isEqualTo(new URI("http://test.com:8080"));
    }
}