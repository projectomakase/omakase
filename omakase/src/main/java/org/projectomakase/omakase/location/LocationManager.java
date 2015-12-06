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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.jcr.Template;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.api.LocationConfigurationException;
import org.projectomakase.omakase.location.spi.LocationConfiguration;
import org.projectomakase.omakase.location.spi.LocationProvider;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.search.SearchException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.net.URI;
import java.util.Optional;

/**
 * Java Facade for managing locations.
 *
 * @author Richard Lucas
 */
@Stateless
public class LocationManager {

    @Inject
    @OrganizationNodePath("locations")
    String locationsNodePath;
    @Inject
    LocationDAO locationDAO;
    @Inject
    LocationProviderResolver locationProviderResolver;
    @Inject
    IdGenerator idGenerator;

    /**
     * Creates a new location that is backed by a location provider implementation.
     *
     * @param location
     *         the location to create.
     * @return a new location that is backed by a location provider implementation.
     */
    public Location createLocation(Location location) {
        try {
            locationProviderResolver.getLocationProvider(location.getType());
        } catch (OmakaseRuntimeException e) {
            throw new LocationConfigurationException("Invalid location type " + location.getType(), e);
        }
        location.setId(idGenerator.getId());
        return locationDAO.create(getLocationPath(Optional.empty()), location);
    }

    /**
     * Returns all of the locations that match the given search constraints.
     *
     * @param search
     *         search constraints
     * @return all of the locations that match the given search constraints.
     * @throws SearchException
     *         if there is an error executing the search.
     */
    public SearchResult<Location> findLocations(final Search search) {
        return locationDAO.findNodes(getLocationPath(Optional.empty()), search, "omakase:location");
    }

    /**
     * Returns the location for the given location id if it exists otherwise returns an empty Optional.
     *
     * @param locationId
     *         the location id.
     * @return the location for the given location id if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the location.
     */
    public Optional<Location> getLocation(final String locationId) {
        if (Strings.isNullOrEmpty(locationId)) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(locationDAO.get(getLocationPath(Optional.of(locationId))));
        }
    }

    /**
     * Deletes the location for the given location id.
     *
     * @param locationId
     *         the location id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the location.
     * @throws NotFoundException
     *         if an location does not exist for the given location id.
     */
    public void deleteLocation(final String locationId) {
        locationDAO.remove(getLocationPath(Optional.of(locationId)));
    }

    /**
     * Returns the {@link LocationConfiguration} type
     *
     * @param locationId
     *         the location id
     * @param <T>
     *         the {@link LocationConfiguration} type
     * @return the {@link LocationConfiguration} type
     */
    public <T extends LocationConfiguration> Class<T> getLocationConfigurationType(final String locationId) {
        LocationProvider<T> locationProvider = getLocationProvider(locationId);
        return locationProvider.getConfigurationType();
    }

    /**
     * Updates the location configuration for the specified location
     *
     * @param locationId
     *         the location id.
     * @param locationConfiguration
     *         the location configuration.
     * @return the location configuration.
     * @throws NotAuthorizedException
     *         if the user does not have access to the location.
     * @throws NotFoundException
     *         if an location does not exist for the given location id.
     */
    public LocationConfiguration updateLocationConfiguration(final String locationId, final LocationConfiguration locationConfiguration) {
        Location currentLocation = getLocation(locationId).orElseThrow(() -> new NotFoundException("Location " + locationId + " does not exist"));
        LocationProvider<LocationConfiguration> locationProvider = getLocationProvider(currentLocation.getId());
        locationProvider.validateConfiguration(locationConfiguration);
        currentLocation.setLocationConfiguration(locationConfiguration);
        Location updatedLocation = locationDAO.update(currentLocation);
        return updatedLocation.getLocationConfiguration();
    }

    /**
     * Returns a set of supported location templates;
     *
     * @return a set of supported location templates;
     */
    public ImmutableSet<Template> getLocationTemplates() {
        return locationProviderResolver.getLocationProviders()
                .stream()
                .map(provider -> new Template(provider.getType(), provider.getConfigurationTemplate()))
                .collect(ImmutableSetCollector.toImmutableSet());
    }

    /**
     * Returns the specified location template
     *
     * @param templateName
     *         the template name
     * @return the specified location template
     * @throws NotFoundException
     *         if the template does not exist
     */
    public Template getLocationTemplate(String templateName) {
        try {
            LocationProvider<LocationConfiguration> provider = locationProviderResolver.getLocationProvider(templateName);
            return new Template(templateName, provider.getConfigurationTemplate());
        } catch (OmakaseRuntimeException e) {
            throw new NotFoundException("template " + templateName + "not found", e);
        }
    }

    /**
     * Returns true if the URI is an Omakase location URI, otherwise false.
     *
     * @param uri
     *         the URI
     * @return true if the URI is an Omakase location URI, otherwise false.
     */
    public boolean isLocationURI(final URI uri) {
        return "oma-location".equalsIgnoreCase(uri.getScheme());
    }

    /**
     * Returns true if the location for the given Omakase location URI exists, otherwise false.
     *
     * @param locationUri
     *         the location URI
     * @return true if the location for the given Omakase location URI exists, otherwise false.
     * @throws OmakaseRuntimeException
     *         if the location URI is not a Omakase location URI.
     */
    public boolean doesLocationExist(final URI locationUri) {
        if (!isLocationURI(locationUri)) {
            throw new OmakaseRuntimeException("the URI provided is not a location URI");
        }
        String locationId = locationUri.getAuthority();
        return getLocation(locationId).isPresent();
    }

    /**
     * Returns the expanded location URI for the given URI.
     * <p>
     * If the URI has the scheme 'oma-location' the provided URI will be resolved and expanded before being returned, otherwise the provided URI will be returned as is.
     * </p>
     *
     * @param uri
     *         the URI to expand
     * @return the expanded location URI for the given URI.
     * @throws NotFoundException
     *         if the location does not exist.
     * @throws LocationConfigurationException
     *         if the location is not configured.
     */
    public URI expandLocationUri(final URI uri) {
        if (isLocationURI(uri)) {
            String locationId = uri.getAuthority();
            Location location = getLocation(locationId).orElseThrow(() -> new NotFoundException("Location " + locationId + " does not exist"));
            Optional.ofNullable(location.getLocationConfiguration()).orElseThrow(() -> new LocationConfigurationException("Location " + locationId + " is not configured"));
            URI locationUri = locationProviderResolver.getLocationProvider(location.getType()).getLocationUri(location);
            String toReplace = uri.getScheme() + "://" + locationId;
            return Throwables.returnableInstance(() -> new URI(uri.toString().replace(toReplace, locationUri.toString())));
        } else {
            return uri;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends LocationConfiguration> LocationProvider<T> getLocationProvider(final String locationId) {
        Location location = getLocation(locationId).orElseThrow(() -> new NotFoundException("Location " + locationId + " does not exist"));
        return (LocationProvider<T>) locationProviderResolver.getLocationProvider(location.getType());
    }

    private String getLocationPath(Optional<String> locationId) {
        return locationId.map(id -> locationsNodePath + "/" + id).orElse(locationsNodePath);
    }
}
