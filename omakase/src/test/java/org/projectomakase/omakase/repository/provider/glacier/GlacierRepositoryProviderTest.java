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
package org.projectomakase.omakase.repository.provider.glacier;

import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class GlacierRepositoryProviderTest {

    private GlacierRepositoryProvider provider;

    @Before
    public void before() {
        provider = new GlacierRepositoryProvider();
        provider.idGenerator = new IdGenerator();
    }

    @Test
    public void shouldThrowExceptionIfSecretKeyContainsForwardSlash() throws Exception {
        assertThatThrownBy(() -> provider.validateConfiguration(new GlacierRepositoryConfiguration("access", "secret/", "us-west-1", "test", "a"))).isInstanceOf(RepositoryConfigurationException.class)
                .hasMessage("AWS secret keys that contain a '/' are not supported, please use a different key");
    }

}