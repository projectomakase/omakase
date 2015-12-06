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
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantType;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class ReplicationJobConfigurationValidatorTest {
    
    private ContentManager contentManager;
    private RepositoryManager repositoryManager;
    private ReplicationJobConfigurationValidator validator;

    @Before
    public void setUp() throws Exception {
        contentManager = mock(ContentManager.class);
        repositoryManager = mock(RepositoryManager.class);

        validator = new ReplicationJobConfigurationValidator();
        validator.contentManager = contentManager;
        validator.repositoryManager = repositoryManager;
    }

    @Test
    public void shouldValidateReplicationJobConfigurationNoErrors() throws Exception {
        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("1"));
            config.setDestinationRepositories(ImmutableList.of("2"));
        });

        doReturn(Optional.of(new Variant())).when(contentManager).getVariant(anyString());
        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(new VariantRepository("1", "a", "FILE")), 1);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Repository repositoryA = new Repository("a", "", "FILE");
        repositoryA.setId("1");
        repositoryA.setRepositoryConfiguration(new FileRepositoryConfiguration());
        doReturn(Optional.of(repositoryA)).when(repositoryManager).getRepository("1");

        Repository repositoryB = new Repository("b", "", "FILE");
        repositoryA.setId("2");
        repositoryB.setRepositoryConfiguration(new FileRepositoryConfiguration());
        doReturn(Optional.of(repositoryB)).when(repositoryManager).getRepository("2");

        assertThat(validator.validate(replicationJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldValidateReplicationJobConfigurationWithErrors() throws Exception {

        doReturn(Optional.empty()).when(contentManager).getVariant(anyString());
        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(), 0);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        RepositoryManager repositoryManager = mock(RepositoryManager.class);
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());

        assertThat(validator.validate(new ReplicationJobConfiguration())).isNotEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenVariantIsInSourceRepo() throws Exception {
        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("1"));
            config.setDestinationRepositories(ImmutableList.of("2"));
        });

        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(new VariantRepository("1", "a", "FILE")), 1);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Assertions.assertThat(ReplicationJobConfigurationValidator.checkReplicationConfigVariantInSourceRepo(contentManager).apply(replicationJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenVariantIsNotInSourceRepo() throws Exception {
        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("a"));
            config.setDestinationRepositories(ImmutableList.of("b"));
        });

        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(), 0);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Assertions.assertThat(ReplicationJobConfigurationValidator
                                      .checkReplicationConfigVariantInSourceRepo(contentManager).apply(replicationJobConfiguration)).contains("The variant is not available in any of the specified repositories");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenVariantIsNotInDestRepo() throws Exception {
        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("a"));
            config.setDestinationRepositories(ImmutableList.of("b"));
        });

        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(), 0);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Assertions.assertThat(ReplicationJobConfigurationValidator.checkReplicationConfigVariantNotInDestRepo(contentManager).apply(replicationJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenVariantIsInDestRepo() throws Exception {
        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("1"));
            config.setDestinationRepositories(ImmutableList.of("2"));
        });

        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(new VariantRepository("2", "b", "FILE")), 1);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Assertions.assertThat(ReplicationJobConfigurationValidator
                                      .checkReplicationConfigVariantNotInDestRepo(contentManager).apply(replicationJobConfiguration)).contains("The variant already exists in the destination repositories [2]");
    }

    @Test
    public void shouldReturnEmptyOptionalIfValidHLSRepository() throws Exception {
        Variant variant = new Variant();
        variant.setType(VariantType.HLS_MANIFEST);
        doReturn(Optional.of(variant)).when(contentManager).getVariant(anyString());
        doReturn(Optional.of(new Repository("a", "", "FILE"))).when(repositoryManager).getRepository(anyString());

        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("a"));
            config.setDestinationRepositories(ImmutableList.of("b"));
        });

        Assertions.assertThat(ReplicationJobConfigurationValidator.checkReplicationConfigHasValidHLSRepository(contentManager, repositoryManager).apply(replicationJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCheckingValidHLSRepositoryAndDoesNotExist() throws Exception {
        Variant variant = new Variant();
        variant.setType(VariantType.HLS_MANIFEST);
        doReturn(Optional.of(variant)).when(contentManager).getVariant(anyString());
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());

        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("a"));
            config.setDestinationRepositories(ImmutableList.of("b"));
        });

        Assertions.assertThat(ReplicationJobConfigurationValidator.checkReplicationConfigHasValidHLSRepository(contentManager, repositoryManager).apply(replicationJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageIfInvalidHLSRepository() throws Exception {
        Variant variant = new Variant();
        variant.setType(VariantType.HLS_MANIFEST);
        doReturn(Optional.of(variant)).when(contentManager).getVariant(anyString());
        doReturn(Optional.of(new Repository("a", "", "GLACIER"))).when(repositoryManager).getRepository(anyString());

        ReplicationJobConfiguration replicationJobConfiguration = ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setSourceRepositories(ImmutableList.of("a"));
            config.setDestinationRepositories(ImmutableList.of("b"));
        });

        Assertions.assertThat(ReplicationJobConfigurationValidator.checkReplicationConfigHasValidHLSRepository(contentManager, repositoryManager).apply(replicationJobConfiguration)).contains("Support for replicating HLS content into Glacier is not implemented");
    }

}