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
package org.projectomakase.omakase.repository.provider.s3;

import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class S3RepositoryProviderTest {

    private S3RepositoryProvider provider;

    @Before
    public void before() {
        provider = new S3RepositoryProvider();
        provider.idGenerator = new IdGenerator();
    }

    @Test
    public void shouldReturnS3RepositoryConfiguration() {
        assertThat(provider.getConfigurationType()).isEqualTo(S3RepositoryConfiguration.class);
    }

    @Test
    public void shouldGetS3RepositoryUri() throws Exception {
        Repository repository = new Repository("s3", "s3", "S3");
        repository.setRepositoryConfiguration(new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", null));
        URI uri = provider.getRepositoryUri(repository);
        assertThat(uri).isEqualTo(new URI("s3://access:secret@test.s3-us-west-1.amazonaws.com"));
    }

    @Test
    public void shouldGetRepositoryUriWithPath() throws Exception {
        Repository repository = new Repository("s3", "s3", S3RepositoryProvider.class.getName());
        repository.setRepositoryConfiguration(new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", "/a1"));
        URI uri = provider.getRepositoryUri(repository);
        assertThat(uri).isEqualTo(new URI("s3://access:secret@test.s3-us-west-1.amazonaws.com/a1"));
    }


    @Test
    public void shouldReturnFileUri() {
        Repository repository = new Repository("s3", "s3", "S3");
        repository.setRepositoryConfiguration(new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", null));
        assertThat(provider.getNewFileUri().get().toString().endsWith(".bin")).isTrue();
    }

    @Test
    public void shouldThrowExceptionIfSecretKeyContainsForwardSlash() throws Exception {
        assertThatThrownBy(() -> provider.validateConfiguration(new S3RepositoryConfiguration("access", "secret/", "us-west-1", "test", null))).isInstanceOf(RepositoryConfigurationException.class)
                .hasMessage("AWS secret keys that contain a '/' are not supported, please use a different key");
    }
}