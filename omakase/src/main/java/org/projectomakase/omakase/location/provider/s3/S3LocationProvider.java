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
package org.projectomakase.omakase.location.provider.s3;

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
 * AWS S3 Location Provider implementation.
 *
 * @author Richard Lucas
 */
public class S3LocationProvider implements LocationProvider<S3LocationConfiguration> {

    private static final String TYPE = "S3";

    @Inject
    JcrTools jcrTools;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Class<S3LocationConfiguration> getConfigurationType() {
        return S3LocationConfiguration.class;
    }

    @Override
    public ImmutableSet<PropertyDefinition> getConfigurationTemplate() {
        return jcrTools.getPropertyDefinitions(S3LocationConfiguration.MIXIN);
    }

    @Override
    public void validateConfiguration(S3LocationConfiguration locationConfiguration) {
        if (locationConfiguration.getAwsSecretKey().contains("/")) {
            throw new LocationConfigurationException("AWS secret keys that contain a '/' are not supported, please use a different key");
        }
        if (locationConfiguration.getRoot() != null && !locationConfiguration.getRoot().startsWith("/")) {
            throw new LocationConfigurationException("root path does not start with /");
        }
    }

    @Override
    public URI getLocationUri(Location location) {
        S3LocationConfiguration configuration = getConfiguration(location);
        String host = configuration.getBucket().toLowerCase() + ".s3-" + configuration.getRegion().toLowerCase() + ".amazonaws.com";
        return Throwables.returnableInstance(() -> new URI("s3", configuration.getAwsAccessKey() + ":" + configuration.getAwsSecretKey(), host, -1, configuration.getRoot(), null, null));
    }

    private static S3LocationConfiguration getConfiguration(Location location) {
        return (S3LocationConfiguration) location.getLocationConfiguration();
    }
}
