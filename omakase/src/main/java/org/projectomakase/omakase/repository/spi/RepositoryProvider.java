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
package org.projectomakase.omakase.repository.spi;

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;

import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;
import java.util.Optional;

/**
 * Extension Point for implementing Repository Provider's for custom Repository types e.g. S3, StorNext HSM etc.
 * <p>
 * Implementations are discovered at runtime and our instantiated as CDI managed beans.
 * </p>
 *
 * @param <T>
 *         the RepositoryConfiguration type used by the repository provider implementation.
 * @author Richard Lucas
 */
public interface RepositoryProvider<T extends RepositoryConfiguration> {

    /**
     * Returns the Repository Provider type.
     *
     * @return the Repository Provider type.
     */
    String getType();

    /**
     * Returns the {@link RepositoryConfiguration} type
     *
     * @return the {@link RepositoryConfiguration} type
     */
    Class<T> getConfigurationType();

    /**
     * Returns a set of {@link javax.jcr.nodetype.PropertyDefinition}s that describe the providers configuration template.
     * <p>
     * The configuration template defines the configuration properties for the provider.
     * </p>
     *
     * @return a set of {@link javax.jcr.nodetype.PropertyDefinition}s that describe the providers configuration template.
     */
    ImmutableSet<PropertyDefinition> getConfigurationTemplate();

    /**
     * Validates the repository configuration
     *
     * @param repositoryConfiguration
     *         the repository configuration
     * @throws RepositoryConfigurationException
     *         if the configuration is invalid
     */
    void validateConfiguration(T repositoryConfiguration);

    /**
     * Returns an absolute URI for the the root of the repository.
     *
     * @param repository
     *         the repository
     * @return an absolute URI for the the root of the repository.
     */
    URI getRepositoryUri(Repository repository);

    /**
     * Returns a URI relative to the root of the repository that can be used to write a new file to within the repository.
     * <p>
     * Some repository providers do not support specifying the file URI because the underlying repository does not allow you to specify one,
     * if this is the case an empty optional is returned.
     * </p>
     *
     * @return a URI relative to the root of the repository that can be used to write a new file to within the repository, or an empty optional if
     * the provider does not support generating a file uri.
     */
    Optional<URI> getNewFileUri();
}
