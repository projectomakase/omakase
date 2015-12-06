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
package org.projectomakase.omakase.location.provider.file;

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.jcr.JcrTools;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.api.LocationConfigurationException;
import org.projectomakase.omakase.location.spi.LocationProvider;

import javax.inject.Inject;
import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;

/**
 * Local File System Location Provider implementation.
 *
 * @author Richard Lucas
 */
public class FileLocationProvider implements LocationProvider<FileLocationConfiguration> {

    private static final String TYPE = "FILE";

    @Inject
    JcrTools jcrTools;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Class<FileLocationConfiguration> getConfigurationType() {
        return FileLocationConfiguration.class;
    }

    @Override
    public ImmutableSet<PropertyDefinition> getConfigurationTemplate() {
        return jcrTools.getPropertyDefinitions(FileLocationConfiguration.MIXIN);
    }

    @Override
    public void validateConfiguration(FileLocationConfiguration locationConfiguration) {
        if (!locationConfiguration.getRoot().startsWith("/")) {
            throw new LocationConfigurationException("root path does not start with /");
        }
    }

    @Override
    public URI getLocationUri(Location location) {
        return Throwables.returnableInstance(() -> new URI("file:" + getConfiguration(location).getRoot()));
    }

    private static FileLocationConfiguration getConfiguration(Location location) {
        return (FileLocationConfiguration) location.getLocationConfiguration();
    }
}
