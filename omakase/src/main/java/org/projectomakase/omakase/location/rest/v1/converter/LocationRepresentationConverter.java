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
package org.projectomakase.omakase.location.rest.v1.converter;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.rest.v1.model.LocationModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Richard Lucas
 */
public class LocationRepresentationConverter implements RepresentationConverter<LocationModel, Location> {

    @Override
    public LocationModel from(UriInfo uriInfo, Location location) {
        LocationModel repositoryModel = new LocationModel();
        repositoryModel.setId(location.getId());
        repositoryModel.setName(location.getLocationName());
        repositoryModel.setDescription(location.getDescription());
        repositoryModel.setType(location.getType());
        repositoryModel.setCreated(ZonedDateTime.ofInstant(location.getCreated().toInstant(), ZoneId.systemDefault()));
        repositoryModel.setCreatedBy(location.getCreatedBy());
        repositoryModel.setLastModified(ZonedDateTime.ofInstant(location.getLastModified().toInstant(), ZoneId.systemDefault()));
        repositoryModel.setLastModifiedBy(location.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getResourceUriAsString(uriInfo, "locations", location.getId())));
        builder.put("configuration", new Href(getResourceUriAsString(uriInfo, "locations", location.getId(), "configuration")));
        builder.put("template", new Href(getResourceUriAsString(uriInfo, "locations", "templates", location.getType())));
        repositoryModel.setLinks(builder.build());
        return repositoryModel;
    }

    @Override
    public Location to(UriInfo uriInfo, LocationModel representation) {
        return new Location(representation.getName(), representation.getDescription(), representation.getType());
    }

    @Override
    public Location update(UriInfo uriInfo, LocationModel locationModel, Location target) {
        return null;
    }
}
