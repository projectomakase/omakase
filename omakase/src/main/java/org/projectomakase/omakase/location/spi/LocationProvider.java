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
package org.projectomakase.omakase.location.spi;

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.api.LocationConfigurationException;

import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;

/**
 * Extension Point for implementing Location Provider's for custom Location types e.g. File, FTP etc.
 * <p>
 * Implementations are discovered at runtime and our instantiated as CDI managed beans.
 * </p>
 *
 * @param <T>
 *         the LocationProvider type used by the location provider implementation.
 * @author Richard Lucas
 */
public interface LocationProvider<T extends LocationConfiguration> {

    /**
     * Returns the Location Provider type.
     *
     * @return the Location Provider type.
     */
    String getType();

    /**
     * Returns the {@link LocationConfiguration} type
     *
     * @return the {@link LocationConfiguration} type
     */
    Class<T> getConfigurationType();

    /**
     * Returns a set of {@link PropertyDefinition}s that describe the providers configuration template.
     * <p>
     * The configuration template defines the configuration properties for the provider.
     * </p>
     *
     * @return a set of {@link PropertyDefinition}s that describe the providers configuration template.
     */
    ImmutableSet<PropertyDefinition> getConfigurationTemplate();

    /**
     * Validates the location configuration
     *
     * @param locationConfiguration
     *         the location configuration
     * @throws LocationConfigurationException
     *         if the location is invalid
     */
    void validateConfiguration(T locationConfiguration);

    /**
     * Returns the fully resolved URI for the given location.
     *
     * @param location the location
     * @return the fully resolved URI for the given location.
     */
    URI getLocationUri(Location location);
}
