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
import org.projectomakase.omakase.commons.functions.ExtendedBiFunction;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A reusable collection of rules that can be applied to job configuration values.
 *
 * @author Richard Lucas
 */
public final class JobConfigurationRules {

    private static final Logger LOGGER = Logger.getLogger(JobConfigurationRules.class);

    static final ImmutableList<String> SUPPORTED_PROTOCOLS = ImmutableList.of("file", "ftp", "sftp", "http", "https", "s3", "oma-location");

    private JobConfigurationRules() {
        // hide default constructor
    }


    static Function<String, Optional<String>> checkVariantIsNotNull() {
        return variantId -> checkExpression(variantId == null, "'variant' is a required property");
    }

    static ExtendedBiFunction<ContentManager, String, Optional<String>> checkVariantExists() {
        return (contentManager, variantId) -> tryAndCatchNotAuthorized(() -> checkExpression(!contentManager.getVariant(variantId).isPresent(), "Variant does not exist or is inaccessible"),
                                                                       "Variant does not exist or is inaccessible");
    }

    static Function<List<String>, Optional<String>> checkRepositoriesIsNotEmpty() {
        return repositoryIds -> checkExpression(repositoryIds.isEmpty(), "One or more repositories must be provided");
    }

    static Function<List<String>, Optional<String>> checkHasSingleRepository() {
        return repositoryIds -> checkExpression(repositoryIds.size() > 1, "Support for multiple repositories is not implemented");
    }

    static ExtendedBiFunction<RepositoryManager, List<String>, Optional<String>> checkRepositoriesExist() {
        return (repositoryManager, repositoryIds) -> {

            if (repositoryIds.isEmpty()) {
                return Optional.empty();
            }

            return tryAndCatchNotAuthorized(() -> {
                Optional<Repository> repository = repositoryManager.getRepository(repositoryIds.get(0));
                return checkExpression(!repository.isPresent(), "Repository does not exist or is inaccessible");
            }, "Repository does not exist or is inaccessible");
        };
    }

    static ExtendedBiFunction<RepositoryManager, List<String>, Optional<String>> checkRepositoryConfigured() {
        return (repositoryManager, repositoryIds) -> {

            if (repositoryIds.isEmpty()) {
                return Optional.empty();
            }

            return tryAndCatchNotAuthorized(() -> {
                Optional<Repository> repository = repositoryManager.getRepository(repositoryIds.get(0));
                if (repository.isPresent()) {
                    return checkExpression(!Optional.ofNullable(repository.get().getRepositoryConfiguration()).isPresent(), "Repository " + repository.get().getId() + " is not configured");
                } else {
                    return Optional.empty();
                }
            }, "Repository does not exist or is inaccessible");
        };
    }

    static Optional<String> checkExpression(boolean expression, String errorMessage) {
        if (expression) {
            return Optional.of(errorMessage);
        } else {
            return Optional.empty();
        }
    }

    @FunctionalInterface
    private interface CheckedExpression {
        Optional<String> apply();
    }

    private static Optional<String> tryAndCatchNotAuthorized(CheckedExpression expression, String notAuthorizedMessage) {
        try {
            return expression.apply();
        } catch (NotAuthorizedException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(e.getMessage(), e);
            }
            return Optional.of(notAuthorizedMessage);
        }
    }
}
