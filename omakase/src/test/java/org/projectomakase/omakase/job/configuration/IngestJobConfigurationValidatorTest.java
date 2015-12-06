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
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration.Builder;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class IngestJobConfigurationValidatorTest {

    private ContentManager contentManager;
    private RepositoryManager repositoryManager;
    private LocationManager locationManager;
    private IngestJobConfigurationValidator validator;

    @Before
    public void setUp() throws Exception {
        contentManager = mock(ContentManager.class);
        repositoryManager = mock(RepositoryManager.class);
        locationManager = mock(LocationManager.class);
        validator = new IngestJobConfigurationValidator();
        validator.contentManager = contentManager;
        validator.repositoryManager = repositoryManager;
        validator.locationManager = locationManager;
    }

    @Test
    public void shouldValidateIngestJobConfigurationNoErrors() throws Exception {
        IngestJobConfiguration ingestJobConfiguration = IngestJobConfiguration.Builder.build(config -> {
            config.setVariant("a");
            config.setRepositories(ImmutableList.of("a"));
            config.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(file -> file.setUri("file:/a.txt"))));
        });

        doReturn(Optional.of(new Variant())).when(contentManager).getVariant(anyString());

        Repository repository = new Repository("a", "", "FILE");
        repository.setRepositoryConfiguration(new FileRepositoryConfiguration());
        doReturn(Optional.of(repository)).when(repositoryManager).getRepository(anyString());

        assertThat(validator.validate(ingestJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldValidateIngestJobConfigurationWithErrors() throws Exception {
        doReturn(Optional.empty()).when(contentManager).getVariant(anyString());
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());
        assertThat(validator.validate(new IngestJobConfiguration())).isNotEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenIngestFilesIsNotEmpty() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigHasIngestFiles().apply(Builder.build(config -> config.setIngestJobFiles(ImmutableList.of(new IngestJobFile()))))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenIngestFilesIsEmpty() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigHasIngestFiles().apply(new IngestJobConfiguration())).contains("One or more files must be provided");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenSingleManifest() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigDoesNotHaveMultipleManifests()
                           .apply(Builder.build(config -> config.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(file -> file.setIsManifest(true))))))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenMultipleManifests() throws Exception {
        List<IngestJobFile> ingestJobFiles = ImmutableList.of(IngestJobFile.Builder.build(file -> file.setIsManifest(true)), IngestJobFile.Builder.build(file -> file.setIsManifest(true)));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigDoesNotHaveMultipleManifests().apply(Builder.build(config -> config.setIngestJobFiles(ingestJobFiles)))).contains(
                "Support for multiple manifests is not implemented");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenHLSManifest() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> {
            file.setUri("file:/a.m3u8");
            file.setIsManifest(true);
        });
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigManifestTypeSupported().apply(Builder.build(config -> config.setIngestJobFiles(ImmutableList.of(ingestJobFile))))).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenUnsupportedManifest() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> {
            file.setUri("file:/a.bad");
            file.setIsManifest(true);
        });
        Assertions.assertThat(IngestJobConfigurationValidator
                                      .checkIngestConfigManifestTypeSupported().apply(Builder.build(config -> config.setIngestJobFiles(ImmutableList.of(ingestJobFile))))).contains("Unsupported manifest");
    }

    @Test
    public void shouldReturnEmptyOptionIfDeleteSourceNotSet() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigDeleteSourceNotSet().apply(new IngestJobConfiguration())).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageIfDeleteSourceSet() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigDeleteSourceNotSet().apply(IngestJobConfiguration.Builder.build(config -> config.setDeleteSource(true))))
                .contains("Support for deleting the source file(s) on ingest in not implemented");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenIngestFileHasURI() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> {
            file.setUri("file:/a.m3u8");
            file.setIsManifest(true);
        });
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHasUri().apply(ingestJobFile)).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageWhenIngestFileHasNullURI() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHasUri().apply(new IngestJobFile())).contains("File URI is a required property");
    }

    @Test
    public void shouldReturnEmptyOptionalWhenIngestFileHasValidSchema() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setUri("file:/a.m3u8"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHasValidSchema().apply(ingestJobFile)).isEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenIngestFileHasValidSchemaCaseInsensitive() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setUri("FILE:/a.m3u8"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHasValidSchema().apply(ingestJobFile)).isEmpty();
    }


    @Test
    public void shouldReturnErrorMessageWhenIngestFileHasNoSchema() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setUri("a.m3u8"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHasValidSchema().apply(ingestJobFile)).contains("Invalid file uri, no scheme specified");
    }

    @Test
    public void shouldReturnErrorMessageWhenIngestFileHasInvalidSchema() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setUri("bad:/a.m3u8"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHasValidSchema().apply(ingestJobFile)).contains("Unsupported file uri protocol bad");
    }

    @Test
    public void shouldReturnErrorMessageWhenIngestFileOmakaseLocationDoesNotExist() throws Exception {
        doReturn(true).when(locationManager).isLocationURI(any(URI.class));
        doReturn(false).when(locationManager).doesLocationExist(any(URI.class));
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setUri("oma-location://123"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileLocationExists(locationManager).apply(ingestJobFile)).contains("Location does not exist");
    }

    @Test
    public void shouldReturnEmptyOptionalNoHashValueOrAlgorithm() throws Exception {
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHashAlgorithmAndValue().apply(new IngestJobFile())).isEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalValidHashValueAndAlgorithm() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> {
            file.setHash("a");
            file.setHashAlgorithm("MD5");
        });
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHashAlgorithmAndValue().apply(new IngestJobFile())).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageNoHashAlgorithm() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setHash("a"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHashAlgorithmAndValue().apply(ingestJobFile)).contains("No hash algorithm specified");
    }

    @Test
    public void shouldReturnErrorMessageNoHashValue() throws Exception {
        IngestJobFile ingestJobFile = IngestJobFile.Builder.build(file -> file.setHashAlgorithm("MD5"));
        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestFileHashAlgorithmAndValue().apply(ingestJobFile)).contains("No hash value specified");
    }

    @Test
    public void shouldReturnEmptyOptionalIfValidHLSRepository() throws Exception {
        doReturn(Optional.of(new Repository("a", "", "FILE"))).when(repositoryManager).getRepository(anyString());

        IngestJobConfiguration ingestJobConfiguration = getIngestJobConfigurationForManfiestContent();

        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigHasValidHLSRepository(repositoryManager).apply(ingestJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCheckingIfValidHLSRepositoryAndItDoesNotExist() throws Exception {
        doReturn(Optional.empty()).when(repositoryManager).getRepository(anyString());

        IngestJobConfiguration ingestJobConfiguration = getIngestJobConfigurationForManfiestContent();

        Assertions.assertThat(IngestJobConfigurationValidator.checkIngestConfigHasValidHLSRepository(repositoryManager).apply(ingestJobConfiguration)).isEmpty();
    }

    @Test
    public void shouldReturnErrorMessageIfInvalidHLSRepository() throws Exception {
        doReturn(Optional.of(new Repository("a", "", "GLACIER"))).when(repositoryManager).getRepository(anyString());

        IngestJobConfiguration ingestJobConfiguration = getIngestJobConfigurationForManfiestContent();

        Assertions.assertThat(IngestJobConfigurationValidator
                                      .checkIngestConfigHasValidHLSRepository(repositoryManager).apply(ingestJobConfiguration)).contains("Support for ingesting HLS content into Glacier is not implemented");
    }

    private IngestJobConfiguration getIngestJobConfigurationForManfiestContent() {
        return Builder.build(config -> {
            config.setVariant("a");
            config.setRepositories(ImmutableList.of("a"));
            config.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(file -> {
                file.setUri("file:/a.m3u8");
                file.setIsManifest(true);
            })));
        });
    }
}