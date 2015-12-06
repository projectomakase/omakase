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
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantRepositorySearchBuilder;
import org.projectomakase.omakase.content.VariantType;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkExpression;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkHasSingleRepository;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoriesExist;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoriesIsNotEmpty;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkRepositoryConfigured;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkVariantExists;
import static org.projectomakase.omakase.job.configuration.JobConfigurationRules.checkVariantIsNotNull;

/**
 * @author Richard Lucas
 */
public class ReplicationJobConfigurationValidator {

    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;

    public List<String> validate(ReplicationJobConfiguration jobConfiguration) {
        ImmutableList.Builder<String> validationErrorsBuilder = ImmutableList.builder();

        validationErrorsBuilder
                .addAll(validateReplicationJobConfiguration(jobConfiguration, contentManager, repositoryManager));

        return validationErrorsBuilder.build();
    }

    private static List<String> validateReplicationJobConfiguration(ReplicationJobConfiguration jobConfiguration, ContentManager contentManager, RepositoryManager repositoryManager) {
        // export config validation functions composed from common validation functions
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigVariantIsNotNull = checkVariantIsNotNull().compose(ReplicationJobConfiguration::getVariant);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigVariantExists = checkVariantExists().curry1(contentManager).compose(ReplicationJobConfiguration::getVariant);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigSourceRepositoriesIsNotEmpty =
                checkRepositoriesIsNotEmpty().compose(ReplicationJobConfiguration::getSourceRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigHasSingleSourceRepository =
                checkHasSingleRepository().compose(ReplicationJobConfiguration::getSourceRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigSourceRepositoriesExist =
                checkRepositoriesExist().curry1(repositoryManager).compose(ReplicationJobConfiguration::getSourceRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigSourceRepositoriesConfigured =
                checkRepositoryConfigured().curry1(repositoryManager).compose(ReplicationJobConfiguration::getSourceRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigDestRepositoriesIsNotEmpty =
                checkRepositoriesIsNotEmpty().compose(ReplicationJobConfiguration::getDestinationRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigHasSingleDestRepository =
                checkHasSingleRepository().compose(ReplicationJobConfiguration::getDestinationRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigDestRepositoriesExist =
                checkRepositoriesExist().curry1(repositoryManager).compose(ReplicationJobConfiguration::getDestinationRepositories);
        Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigDestRepositoriesConfigured =
                checkRepositoryConfigured().curry1(repositoryManager).compose(ReplicationJobConfiguration::getSourceRepositories);

        ImmutableList.Builder<Function<ReplicationJobConfiguration, Optional<String>>> rulesBuilder = ImmutableList.builder();
        rulesBuilder.add(checkReplicationConfigVariantIsNotNull);
        rulesBuilder.add(checkReplicationConfigVariantExists);
        rulesBuilder.add(checkReplicationConfigSourceRepositoriesIsNotEmpty);
        rulesBuilder.add(checkReplicationConfigHasSingleSourceRepository);
        rulesBuilder.add(checkReplicationConfigSourceRepositoriesExist);
        rulesBuilder.add(checkReplicationConfigSourceRepositoriesConfigured);
        rulesBuilder.add(checkReplicationConfigDestRepositoriesIsNotEmpty);
        rulesBuilder.add(checkReplicationConfigHasSingleDestRepository);
        rulesBuilder.add(checkReplicationConfigDestRepositoriesExist);
        rulesBuilder.add(checkReplicationConfigDestRepositoriesConfigured);
        rulesBuilder.add(checkReplicationConfigVariantInSourceRepo(contentManager));
        rulesBuilder.add(checkReplicationConfigVariantNotInDestRepo(contentManager));
        rulesBuilder.add(checkReplicationConfigHasValidHLSRepository(contentManager, repositoryManager));

        List<Function<ReplicationJobConfiguration, Optional<String>>> rules = rulesBuilder.build();

        return rules
                .stream()
                .map(rule -> rule.apply(jobConfiguration))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList());
    }

    static Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigVariantInSourceRepo(ContentManager contentManager) {
        return jobConfiguration -> {
            List<VariantRepository> variantRepositories = contentManager.findVariantRepositories(jobConfiguration.getVariant(), new VariantRepositorySearchBuilder().build()).getRecords();

            boolean isInRepo = variantRepositories
                    .stream()
                    .filter(variantRepository -> variantRepository.getId().equals(jobConfiguration.getSourceRepositories().get(0)))
                    .findAny()
                    .isPresent();

            return checkExpression(!isInRepo, "The variant is not available in any of the specified repositories");
        };
    }

    static Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigVariantNotInDestRepo(ContentManager contentManager) {
        return jobConfiguration -> {
            List<VariantRepository> variantRepositories = contentManager.findVariantRepositories(jobConfiguration.getVariant(), new VariantRepositorySearchBuilder().build()).getRecords();

            List<VariantRepository> duplicateRepositories = variantRepositories
                    .stream()
                    .filter(variantRepository -> variantRepository.getId().equals(jobConfiguration.getDestinationRepositories().get(0)))
                    .collect(toImmutableList());

            return checkExpression(!duplicateRepositories.isEmpty(), "The variant already exists in the destination repositories [" +
                    duplicateRepositories.stream().map(VariantRepository::getId).collect(Collectors.joining(", ")) + "]");
        };
    }

    static Function<ReplicationJobConfiguration, Optional<String>> checkReplicationConfigHasValidHLSRepository(ContentManager contentManager, RepositoryManager repositoryManager) {
        return jobConfiguration -> {

            if (jobConfiguration.getDestinationRepositories().isEmpty()) {
                return Optional.empty();
            }

            boolean isManifestIngest = contentManager.getVariant(jobConfiguration.getVariant()).get().getType().equals(VariantType.HLS_MANIFEST);

            Optional<Repository> repository = repositoryManager.getRepository(jobConfiguration.getDestinationRepositories().get(0));
            if (repository.isPresent()) {
                return checkExpression(isManifestIngest && "GLACIER".equalsIgnoreCase(repository.get().getType()),"Support for replicating HLS content into Glacier is not implemented");
            } else {
                return Optional.empty();
            }
        };
    }
}
