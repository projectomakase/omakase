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
package org.projectomakase.omakase.repository;

import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.projectomakase.omakase.repository.spi.RepositoryProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * @author Richard Lucas
 */
@ApplicationScoped
public class RepositoryProviderResolver {

    private Map<String, RepositoryProvider> cachedProviders = new HashMap<>();

    @Inject
    @Any
    Instance<RepositoryProvider<? extends RepositoryConfiguration>> repositoryProviders;


    @SuppressWarnings("unchecked")
    public <T extends RepositoryConfiguration> RepositoryProvider<T> getRepositoryProvider(String repositoryType) {
        if (!cachedProviders.containsKey(repositoryType)) {
            RepositoryProvider provider = StreamSupport.stream(repositoryProviders.spliterator(), false).filter(repositoryProvider -> repositoryProvider.getType().equals(repositoryType)).findFirst()
                    .orElseThrow(() -> new OmakaseRuntimeException("Unsupported repository provider " + repositoryType));
            cachedProviders.put(repositoryType, provider);
        }
        return cachedProviders.get(repositoryType);

    }

    @SuppressWarnings("unchecked")
    public <T extends RepositoryConfiguration> Set<RepositoryProvider<T>> getRepositoryProviders() {
        return StreamSupport.stream(repositoryProviders.spliterator(), false).map(repositoryProvider -> (RepositoryProvider<T>) repositoryProvider).collect(ImmutableSetCollector.toImmutableSet());
    }
}
