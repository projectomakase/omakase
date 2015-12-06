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
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantRepositorySearchBuilder;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.repository.RepositoryManager;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.SUPPORTED_PROTOCOLS;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkExpression;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkHasSingleRepository;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoriesExist;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoriesIsNotEmpty;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoryConfigured;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkVariantExists;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkVariantIsNotNull;

/**
 * {@link ExportJobConfiguration} validator.
 *
 * @author Richard Lucas
 */
public class ExportJobConfigurationValidator {

    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    LocationManager locationManager;

    /**
     * Validates the given {@link ExportJobConfiguration}.
     *
     * @param jobConfiguration
     *         the {@link ExportJobConfiguration}
     * @return a list of validation error messages
     */
    public List<String> validate(ExportJobConfiguration jobConfiguration) {
        ImmutableList.Builder<String> validationErrorsBuilder = ImmutableList.builder();

        validationErrorsBuilder
                .addAll(validateExportJobConfiguration(jobConfiguration, contentManager, repositoryManager))
                .addAll(validateExportJobConfigurationLocations(jobConfiguration, locationManager));

        return validationErrorsBuilder.build();
    }

    private static List<String> validateExportJobConfiguration(ExportJobConfiguration jobConfiguration, ContentManager contentManager, RepositoryManager repositoryManager) {
        // export config validation functions composed from common validation functions
        Function<ExportJobConfiguration, Optional<String>> checkExportConfigVariantIsNotNull = checkVariantIsNotNull().compose(ExportJobConfiguration::getVariant);
        Function<ExportJobConfiguration, Optional<String>> checkExportConfigVariantExists = checkVariantExists().curry1(contentManager).compose(ExportJobConfiguration::getVariant);
        Function<ExportJobConfiguration, Optional<String>> checkExportConfigRepositoriesIsNotEmpty = checkRepositoriesIsNotEmpty().compose(ExportJobConfiguration::getRepositories);
        Function<ExportJobConfiguration, Optional<String>> checkExportConfigHasSingleRepository = checkHasSingleRepository().compose(ExportJobConfiguration::getRepositories);
        Function<ExportJobConfiguration, Optional<String>> checkExportConfigRepositoriesExist = checkRepositoriesExist().curry1(repositoryManager).compose(ExportJobConfiguration::getRepositories);
        Function<ExportJobConfiguration, Optional<String>> checkExportConfigRepositoriesConfigured =
                checkRepositoryConfigured().curry1(repositoryManager).compose(ExportJobConfiguration::getRepositories);

        List<Function<ExportJobConfiguration, Optional<String>>> rules = ImmutableList.of(
                checkExportConfigVariantIsNotNull,
                checkExportConfigVariantExists,
                checkExportConfigRepositoriesIsNotEmpty,
                checkExportConfigHasSingleRepository,
                checkExportConfigRepositoriesExist,
                checkExportConfigRepositoriesConfigured,
                checkExportConfigVariantInRepo(contentManager),
                checkExportConfigHasLocations(),
                checkHasSingleLocation(),
                checkExportConfigValidateSourceNotSet());

        return rules
                .stream()
                .map(rule -> rule.apply(jobConfiguration))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableListCollector.toImmutableList());
    }

    private static List<String> validateExportJobConfigurationLocations(ExportJobConfiguration jobConfiguration, LocationManager locationManager) {
        List<Function<String, Optional<String>>> rules = ImmutableList.of(
                checkLocationHasValidSchema(),
                checkLocationExists(locationManager));

        Function<String, List<String>> validateLocation = location -> rules
                .stream()
                .map(rule -> rule.apply(location))
                .filter(Optional::isPresent)
                .map(Optional::get).collect(ImmutableListCollector.toImmutableList());

        return jobConfiguration.getLocations()
                .stream()
                .map(validateLocation)
                .collect(ImmutableListsCollector.toImmutableList());
    }

    // export config specific validation functions

    static Function<ExportJobConfiguration, Optional<String>> checkExportConfigVariantInRepo(ContentManager contentManager) {
        return jobConfiguration -> {
            List<VariantRepository> variantRepositories = contentManager.findVariantRepositories(jobConfiguration.getVariant(), new VariantRepositorySearchBuilder().build()).getRecords();

            boolean isInRepo = variantRepositories
                    .stream()
                    .filter(variantRepository -> variantRepository.getId().equals(jobConfiguration.getRepositories().get(0)))
                    .findAny()
                    .isPresent();

            return checkExpression(!isInRepo, "The variant is not available in any of the specified repositories");
        };
    }

    static Function<ExportJobConfiguration, Optional<String>> checkExportConfigHasLocations() {
        return jobConfiguration -> checkExpression(jobConfiguration.getLocations().isEmpty(), "One or more locations must be provided");
    }

    static Function<ExportJobConfiguration, Optional<String>> checkHasSingleLocation() {
        return jobConfiguration -> checkExpression(jobConfiguration.getLocations().size() > 1, "Support for multiple locations is not implemented");
    }

    static Function<ExportJobConfiguration, Optional<String>> checkExportConfigValidateSourceNotSet() {
        return jobConfiguration -> checkExpression(jobConfiguration.getValidate(), "Support for validation in not implemented");
    }

    static Function<String, Optional<String>> checkLocationHasValidSchema() {
        return location -> {
            String scheme = Throwables.returnableInstance(() -> new URI(location).getScheme());
            if (scheme == null) {
                return Optional.of("Invalid location uri, no scheme specified");
            } else {
                return checkExpression(!SUPPORTED_PROTOCOLS.contains(scheme.toLowerCase()), "Unsupported location uri protocol " + scheme);
            }
        };
    }

    static Function<String, Optional<String>> checkLocationExists(LocationManager locationManager) {
        return location -> {
            URI locationUri = Throwables.returnableInstance(() -> new URI(location));
            if (locationManager.isLocationURI(locationUri)) {
                return checkExpression(!locationManager.doesLocationExist(locationUri), "Location does not exist");
            } else {
                return Optional.empty();
            }
        };
    }
}
