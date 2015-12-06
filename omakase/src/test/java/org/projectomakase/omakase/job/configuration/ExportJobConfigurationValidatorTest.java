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
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class ExportJobConfigurationValidatorTest {

    private ContentManager contentManager;
    private RepositoryManager repositoryManager;
    private LocationManager locationManager;
    private ExportJobConfigurationValidator validator;

    @Before
    public void setUp() throws Exception {
        contentManager = mock(ContentManager.class);
        repositoryManager = mock(RepositoryManager.class);
        locationManager = mock(LocationManager.class);
        validator = new ExportJobConfigurationValidator();
        validator.contentManager = contentManager;
        validator.repositoryManager = repositoryManager;
        validator.locationManager = locationManager;
    }

    @Test
    public void shouldValidateExportJobConfigurationNoErrors() throws Exception {
        ExportJobConfiguration exportJobConfiguration = ExportJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setRepositories(ImmutableList.of("1"));
            config.setLocations(ImmutableList.of("file:/out"));
        });

        doReturn(Optional.of(new Variant())).when(contentManager).getVariant(anyString());
        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(new VariantRepository("1", "a", "FILE")), 1);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Repository repository = new Repository("a", "", "FILE");
        repository.setRepositoryConfiguration(new FileRepositoryConfiguration());
        doReturn(Optional.of(repository)).when(repositoryManager).getRepository(anyString());

        assertThat(validator.validate(exportJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldValidateExportJobConfigurationWithErrors() throws Exception {
        doReturn(Optional.empty()).when(contentManager).getVariant(anyString());
        doReturn(new SearchResult<>(ImmutableList.of(), 0)).when(contentManager).findVariantRepositories(anyString(), any(Search.class));
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());

        assertThat(validator.validate(new ExportJobConfiguration())).isNotEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenVariantIsInRepo() throws Exception {
        ExportJobConfiguration exportJobConfiguration = ExportJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setRepositories(ImmutableList.of("1"));
            config.setLocations(ImmutableList.of("file:/out"));
        });

        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(new VariantRepository("1", "a", "FILE")), 1);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Assertions.assertThat(ExportJobConfigurationValidator.checkExportConfigVariantInRepo(contentManager).apply(exportJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenVariantIsNotInRepo() throws Exception {
        ExportJobConfiguration exportJobConfiguration = ExportJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setRepositories(ImmutableList.of("1"));
            config.setLocations(ImmutableList.of("file:/out"));
        });

        SearchResult<VariantRepository> variantRepositorySearchResult = new SearchResult<>(ImmutableList.of(), 0);
        doReturn(variantRepositorySearchResult).when(contentManager).findVariantRepositories(anyString(), any(Search.class));

        Assertions.assertThat(ExportJobConfigurationValidator
                                      .checkExportConfigVariantInRepo(contentManager).apply(exportJobConfiguration)).contains("The variant is not available in any of the specified repositories");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenLocationsAreNotEmpty() throws Exception {
        Assertions.assertThat(ExportJobConfigurationValidator.checkExportConfigHasLocations().apply(ExportJobConfiguration.Builder.build(config -> config.setLocations(ImmutableList.of("file:/out"))))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenLocationsAreEmpty() throws Exception {
        Assertions.assertThat(ExportJobConfigurationValidator.checkExportConfigHasLocations().apply(new ExportJobConfiguration())).contains("One or more locations must be provided");
    }

    @Test
    public void shouldReturnErrorMessageWhenOmakaseLocationDoesNotExist() throws Exception {
        doReturn(true).when(locationManager).isLocationURI(any(URI.class));
        doReturn(false).when(locationManager).doesLocationExist(any(URI.class));
        Assertions.assertThat(ExportJobConfigurationValidator.checkLocationExists(locationManager).apply("oma-location://123")).contains("Location does not exist");
    }



}