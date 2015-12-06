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
package org.projectomakase.omakase.location;

import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.location.spi.LocationConfiguration;
import org.projectomakase.omakase.location.spi.LocationProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Location.
 *
 * @author Richard Lucas
 */
@ApplicationScoped
public class LocationProviderResolver {

    private Map<String, LocationProvider> cachedProviders = new HashMap<>();

    @Inject
    @Any
    Instance<LocationProvider<? extends LocationConfiguration>> locationProviders;


    @SuppressWarnings("unchecked")
    public <T extends LocationConfiguration> LocationProvider<T> getLocationProvider(String locationType) {
        if (!cachedProviders.containsKey(locationType)) {

            LocationProvider provider = StreamSupport.stream(locationProviders.spliterator(), false)
                    .filter(p -> p.getType().equals(locationType))
                    .findFirst()
                    .orElseThrow(() -> new OmakaseRuntimeException("Unsupported location provider " + locationType));

            cachedProviders.put(locationType, provider);
        }
        return cachedProviders.get(locationType);

    }

    @SuppressWarnings("unchecked")
    public <T extends LocationConfiguration> Set<LocationProvider<T>> getLocationProviders() {
        return StreamSupport.stream(locationProviders.spliterator(), false)
                .map(p -> (LocationProvider<T>) p)
                .collect(ImmutableSetCollector.toImmutableSet());
    }
}
