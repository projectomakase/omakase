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
package org.projectomakase.omakase.location.provider.net;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.jcr.JcrTools;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.spi.LocationProvider;

import javax.inject.Inject;
import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;
import java.util.Optional;

/**
 * @author Richard Lucas
 */
public abstract class AbstractNetLocationProvider implements LocationProvider<NetLocationConfiguration> {

    @Inject
    JcrTools jcrTools;

    @Override
    public Class<NetLocationConfiguration> getConfigurationType() {
        return NetLocationConfiguration.class;
    }

    @Override
    public ImmutableSet<PropertyDefinition> getConfigurationTemplate() {
        return jcrTools.getPropertyDefinitions(NetLocationConfiguration.MIXIN);
    }

    @Override
    public void validateConfiguration(NetLocationConfiguration locationConfiguration) {
        // no-op
    }

    @Override
    public URI getLocationUri(Location location) {
        NetLocationConfiguration configuration = getConfiguration(location);

        String userInfo = Optional.of(configuration)
                .filter(config -> configuration.getUsername() != null && config.getPassword() != null)
                .map(config -> config.getUsername() + ":" + config.getPassword())
                .orElse(null);

        String path = Optional.ofNullable(configuration.getRoot()).orElse(null);

        return Throwables.returnableInstance(() -> new URI(getType().toLowerCase(), userInfo, configuration.getAddress(), Ints.checkedCast(configuration.getPort()), path, null, null));
    }

    private static NetLocationConfiguration getConfiguration(Location location) {
        return (NetLocationConfiguration) location.getLocationConfiguration();
    }
}
