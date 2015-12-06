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
package org.projectomakase.omakase.job.configuration;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.junit.Test;

import java.util.Optional;

import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkHasSingleRepository;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoriesExist;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoriesIsNotEmpty;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoryConfigured;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkVariantExists;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkVariantIsNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class JobConfigurationRulesTest {

    @Test
    public void shouldReturnEmptyOptionalWhenVariantIdIsNotNull() throws Exception {
        assertThat(checkVariantIsNotNull().apply("a")).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenNullVariantId() throws Exception {
        assertThat(checkVariantIsNotNull().apply(null)).contains("'variant' is a required property");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenVariantExists() throws Exception {
        ContentManager contentManager = mock(ContentManager.class);
        doReturn(Optional.of(new Variant())).when(contentManager).getVariant(anyString());
        assertThat(checkVariantExists().apply(contentManager, "a")).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenVariantDoesNotExist() throws Exception {
        ContentManager contentManager = mock(ContentManager.class);
        doReturn(Optional.empty()).when(contentManager).getVariant(anyString());
        assertThat(checkVariantExists().apply(contentManager, "a")).contains("Variant does not exist or is inaccessible");
    }

    @Test
    public void shouldReturnErrorMessageWhenVariantIsNotAccessible() throws Exception {
        ContentManager contentManager = mock(ContentManager.class);
        doThrow(new NotAuthorizedException()).when(contentManager).getVariant(anyString());
        assertThat(checkVariantExists().apply(contentManager, "a")).contains("Variant does not exist or is inaccessible");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenRepositoryNamesIsNotEmpty() throws Exception {
        assertThat(checkRepositoriesIsNotEmpty().apply(ImmutableList.of("a"))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenEmptyRepositoryNames() throws Exception {
        assertThat(checkRepositoriesIsNotEmpty().apply(ImmutableList.of())).contains("One or more repositories must be provided");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenNoMultipleRepositoryNames() throws Exception {
        assertThat(checkHasSingleRepository().apply(ImmutableList.of("a"))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenMultipleRepositoryNames() throws Exception {
        assertThat(checkHasSingleRepository().apply(ImmutableList.of("a", "b"))).contains("Support for multiple repositories is not implemented");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenRepositoryExists() throws Exception {
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        doReturn(Optional.of(new Repository())).when(repositoryManager).getRepository(anyString());
        assertThat(checkRepositoriesExist().apply(repositoryManager, ImmutableList.of("a"))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenRepositoryDoesNotExist() throws Exception {
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());
        assertThat(checkRepositoriesExist().apply(repositoryManager, ImmutableList.of("a"))).contains("Repository does not exist or is inaccessible");
    }

    @Test
    public void shouldReturnErrorMessageWhenRepositoryIsNotAccessible() throws Exception {
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        doThrow(new NotAuthorizedException()).when(repositoryManager).getRepository(anyString());
        assertThat(checkRepositoriesExist().apply(repositoryManager, ImmutableList.of("a"))).contains("Repository does not exist or is inaccessible");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCheckingIfIsRepositoryConfiguredAndItDoesNotExist() throws Exception {
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        Repository repository = new Repository();
        repository.setRepositoryConfiguration(new FileRepositoryConfiguration());
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());
        assertThat(checkRepositoryConfigured().apply(repositoryManager, ImmutableList.of("a"))).isEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenRepositoryConfigured() throws Exception {
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        Repository repository = new Repository();
        repository.setRepositoryConfiguration(new FileRepositoryConfiguration());
        doReturn(Optional.of(repository)).when(repositoryManager).getRepository(anyString());
        assertThat(checkRepositoryConfigured().apply(repositoryManager, ImmutableList.of("a"))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenRepositoryIsNotConfigured() throws Exception {
        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        Repository repository = new Repository("a", "", "FILE");
        repository.setId("1");
        doReturn(Optional.of(repository)).when(repositoryManager).getRepository(anyString());
        assertThat(checkRepositoryConfigured().apply(repositoryManager, ImmutableList.of("a"))).contains("Repository 1 is not configured");
    }
}