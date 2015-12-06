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
package org.projectomakase.omakase.repository.provider.file;

import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class FileRepositoryProviderTest {

    private FileRepositoryProvider provider;
    private IdGenerator idGenerator;

    @Before
    public void before() {
        provider = new FileRepositoryProvider();
        idGenerator = mock(IdGenerator.class);
        provider.idGenerator = idGenerator;
    }

    @Test
    public void shouldReturnFilesystemRepositoryConfiguration() {
        assertThat(provider.getConfigurationType()).isEqualTo(FileRepositoryConfiguration.class);
    }

    @Test
    public void shouldValidateConfigurationSuccessfully() {
        FileRepositoryConfiguration configuration = new FileRepositoryConfiguration("/test");
        try {
            provider.validateConfiguration(configuration);
        } catch (RepositoryConfigurationException e) {
            fail("Repository configuration validation failed, expected it to succeed");
        }
    }

    @Test
    public void shouldThrowInvalidRootPathWhenValidatingConfiguration() throws Exception {
        FileRepositoryConfiguration configuration = new FileRepositoryConfiguration("test");
        assertThatThrownBy(() -> provider.validateConfiguration(configuration)).isExactlyInstanceOf(RepositoryConfigurationException.class).hasMessage("root path does not start with /");
    }

    @Test
    public void shouldGetRepositoryUri() throws Exception {
        Repository repository = new Repository("file", "file", "FILE");
        repository.setRepositoryConfiguration(new FileRepositoryConfiguration("/test"));
        URI uri = provider.getRepositoryUri(repository);
        assertThat(uri).isEqualTo(new URI("file:/test"));
    }

    @Test
    public void shouldReturnFileUri() throws Exception {
        doReturn("79b57e23_92b3_49fb_9692_9632ae03d673").when(idGenerator).getId();
        Repository repository = new Repository("file", "file", "FILE");
        repository.setRepositoryConfiguration(new FileRepositoryConfiguration("/test"));
        assertThat(provider.getNewFileUri()).isPresent().contains(new URI("79/b5/7e/79b57e23_92b3_49fb_9692_9632ae03d673.bin"));
    }
}