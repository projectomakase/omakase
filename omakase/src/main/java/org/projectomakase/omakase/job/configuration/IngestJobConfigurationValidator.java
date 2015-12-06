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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkExpression;

/**
 * {@link IngestJobConfiguration} validator.
 *
 * @author Richard Lucas
 */
public class IngestJobConfigurationValidator {

    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    LocationManager locationManager;

    /**
     * Validates the given {@link IngestJobConfiguration}.
     *
     * @param jobConfiguration
     *         the {@link IngestJobConfiguration}
     * @return a {@link List} of validation errors
     */
    public List<String> validate(IngestJobConfiguration jobConfiguration) {
        ImmutableList.Builder<String> validationErrorsBuilder = ImmutableList.builder();

        validationErrorsBuilder
                .addAll(validateIngestJobConfiguration(jobConfiguration, contentManager, repositoryManager))
                .addAll(validateIngestJobConfigurationFiles(jobConfiguration, locationManager));

        return validationErrorsBuilder.build();
    }

    private static List<String> validateIngestJobConfiguration(IngestJobConfiguration jobConfiguration, ContentManager contentManager, RepositoryManager repositoryManager) {

        // ingest config validation functions composed from common validation functions
        Function<IngestJobConfiguration, Optional<String>> checkIngestConfigVariantIsNotNull = JobConfigurationRules.checkVariantIsNotNull().compose(IngestJobConfiguration::getVariant);
        Function<IngestJobConfiguration, Optional<String>> checkIngestConfigVariantExists =
                JobConfigurationRules.checkVariantExists().curry1(contentManager).compose(IngestJobConfiguration::getVariant);
        Function<IngestJobConfiguration, Optional<String>> checkIngestConfigRepositoriesIsNotEmpty =
                JobConfigurationRules.checkRepositoriesIsNotEmpty().compose(IngestJobConfiguration::getRepositories);
        Function<IngestJobConfiguration, Optional<String>> checkIngestConfigHasSingleRepository = JobConfigurationRules.checkHasSingleRepository().compose(IngestJobConfiguration::getRepositories);
        Function<IngestJobConfiguration, Optional<String>> checkIngestConfigRepositoriesExist =
                JobConfigurationRules.checkRepositoriesExist().curry1(repositoryManager).compose(IngestJobConfiguration::getRepositories);
        Function<IngestJobConfiguration, Optional<String>> checkIngestConfigRepositoriesConfigured =
                JobConfigurationRules.checkRepositoryConfigured().curry1(repositoryManager).compose(IngestJobConfiguration::getRepositories);

        List<Function<IngestJobConfiguration, Optional<String>>> rules = ImmutableList.of(
                checkIngestConfigVariantIsNotNull,
                checkIngestConfigVariantExists,
                checkIngestConfigRepositoriesIsNotEmpty,
                checkIngestConfigHasSingleRepository,
                checkIngestConfigRepositoriesExist,
                checkIngestConfigRepositoriesConfigured,
                checkIngestConfigHasIngestFiles(),
                checkIngestConfigDoesNotHaveMultipleManifests(),
                checkIngestConfigManifestTypeSupported(),
                checkIngestConfigDeleteSourceNotSet(),
                checkIngestConfigHasValidHLSRepository(repositoryManager));

        return rules
                .stream()
                .map(rule -> rule.apply(jobConfiguration))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableListCollector.toImmutableList());
    }

    private static List<String> validateIngestJobConfigurationFiles(IngestJobConfiguration jobConfiguration, LocationManager locationManager) {
        List<Function<IngestJobFile, Optional<String>>> rules = ImmutableList.of(
                checkIngestFileHasUri(),
                checkIngestFileHasValidSchema(),
                checkIngestFileLocationExists(locationManager),
                checkIngestFileHashAlgorithmAndValue());

        Function<IngestJobFile, List<String>> validateIngestJobFile = ingestJobFile -> rules
                .stream()
                .map(rule -> rule.apply(ingestJobFile))
                .filter(Optional::isPresent)
                .map(Optional::get).collect(ImmutableListCollector.toImmutableList());

        return jobConfiguration.getIngestJobFiles()
                .stream()
                .map(validateIngestJobFile)
                .collect(ImmutableListsCollector.toImmutableList());
    }

    // ingest config specific validation functions

    static Function<IngestJobConfiguration, Optional<String>> checkIngestConfigHasIngestFiles() {
        return jobConfiguration -> checkExpression(jobConfiguration.getIngestJobFiles().isEmpty(), "One or more files must be provided");
    }

    static Function<IngestJobConfiguration, Optional<String>> checkIngestConfigDoesNotHaveMultipleManifests() {
        return jobConfiguration -> {
            List<IngestJobFile> manifestFiles = jobConfiguration.getIngestJobFiles()
                    .stream()
                    .filter(IngestJobFile::getIsManifest)
                    .collect(ImmutableListCollector.toImmutableList());

            return checkExpression(manifestFiles.size() > 1, "Support for multiple manifests is not implemented");
        };
    }

    static Function<IngestJobConfiguration, Optional<String>> checkIngestConfigManifestTypeSupported() {
        return jobConfiguration -> checkExpression(jobConfiguration.isManifestIngest() && !jobConfiguration.getManifestType().isPresent(), "Unsupported manifest");
    }

    static Function<IngestJobConfiguration, Optional<String>> checkIngestConfigDeleteSourceNotSet() {
        return jobConfiguration -> checkExpression(jobConfiguration.getDeleteSource(), "Support for deleting the source file(s) on ingest in not implemented");
    }

    static Function<IngestJobConfiguration, Optional<String>> checkIngestConfigHasValidHLSRepository(RepositoryManager repositoryManager) {
        return jobConfiguration -> {

            if (jobConfiguration.getRepositories().isEmpty()) {
                return Optional.empty();
            }

            Optional<Repository> repository = repositoryManager.getRepository(jobConfiguration.getRepositories().get(0));
            if (repository.isPresent()) {
                return checkExpression(jobConfiguration.isManifestIngest() && "GLACIER".equalsIgnoreCase(repository.get().getType()), "Support for ingesting HLS content into Glacier is not implemented");
            } else {
                return Optional.empty();
            }
        };
    }

    // ingest file specific validation functions

    static Function<IngestJobFile, Optional<String>> checkIngestFileHasUri() {
        return ingestJobFile -> checkExpression(ingestJobFile.getUri() == null, "File URI is a required property");
    }

    static Function<IngestJobFile, Optional<String>> checkIngestFileHasValidSchema() {
        return ingestJobFile -> {
            String scheme = Throwables.returnableInstance(() -> new URI(ingestJobFile.getUri()).getScheme());
            if (scheme == null) {
                return Optional.of("Invalid file uri, no scheme specified");
            } else {
                return checkExpression(!JobConfigurationRules.SUPPORTED_PROTOCOLS.contains(scheme.toLowerCase()), "Unsupported file uri protocol " + scheme);
            }
        };
    }

    static Function<IngestJobFile, Optional<String>> checkIngestFileLocationExists(LocationManager locationManager) {
        return ingestJobFile -> {
            URI locationUri = Throwables.returnableInstance(() -> new URI(ingestJobFile.getUri()));
            if (locationManager.isLocationURI(locationUri)) {
                return checkExpression(!locationManager.doesLocationExist(locationUri), "Location does not exist");
            } else {
                return Optional.empty();
            }
        };
    }

    static Function<IngestJobFile, Optional<String>> checkIngestFileHashAlgorithmAndValue() {
        return IngestJobConfigurationValidator::validateHashAlgorithmAndValue;
    }

    private static Optional<String> validateHashAlgorithmAndValue(IngestJobFile ingestJobFile) {
        if (!Strings.isNullOrEmpty(ingestJobFile.getHash()) && Strings.isNullOrEmpty(ingestJobFile.getHashAlgorithm())) {
            return Optional.of("No hash algorithm specified");
        } else if (!Strings.isNullOrEmpty(ingestJobFile.getHashAlgorithm()) && Strings.isNullOrEmpty(ingestJobFile.getHash())) {
            return Optional.of("No hash value specified");
        } else {
            return Optional.empty();
        }
    }
}
