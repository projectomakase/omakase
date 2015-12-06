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
package org.projectomakase.omakase.location.rest.v1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.jcr.Template;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.spi.LocationConfiguration;
import org.projectomakase.omakase.location.rest.v1.converter.LocationQuerySearchConverter;
import org.projectomakase.omakase.location.rest.v1.interceptor.PutLocationConfig;
import org.projectomakase.omakase.location.rest.v1.model.LocationModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.model.v1.PaginatedEnvelope;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.projectomakase.omakase.rest.model.v1.TemplateModel;
import org.projectomakase.omakase.rest.pagination.v1.PaginationLinks;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.spi.LinkHeaders;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

/**
 * Location Management JAX-RS Resource implementation. Exposes REST API operations for managing locations.
 *
 * @author Richard Lucas
 */
@Path("locations")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class LocationResource {

    @Inject
    LocationManager locationManager;
    @Inject
    RepresentationConverter<LocationModel, Location> representationConverter;
    @Inject
    LocationQuerySearchConverter querySearchConverter;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    @GET
    @GZIP
    public Response getLocations(@QueryParam("page") @DefaultValue("1") int page, @QueryParam("per_page") @DefaultValue("10") int perPage,
                                 @DefaultValue("false") @QueryParam("only_count") boolean onlyCount) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(true);
        Search search = querySearchConverter.from(queryParams);
        SearchResult<Location> searchResult = locationManager.findLocations(search);
        if (!onlyCount) {
            Collection<LocationModel> jobModels = representationConverter.from(uriInfo, searchResult.getRecords());
            PaginationLinks paginationLinks = new PaginationLinks(uriInfo, page, perPage, searchResult.getTotalRecords());
            PaginatedEnvelope<LocationModel> envelope =
                    new PaginatedEnvelope<>(page, perPage, paginationLinks.getTotalPages(), searchResult.getTotalRecords(), jobModels, paginationLinks.get());
            LinkHeaders linkHeaders = new LinkHeaders();
            paginationLinks.get().forEach((rel, href) -> linkHeaders.addLink(Link.fromUri(href.getHref()).rel(rel).build()));
            return Response.ok(envelope).links(linkHeaders.getLinks().toArray(new Link[linkHeaders.getLinks().size()])).build();
        } else {
            PaginatedEnvelope<LocationModel> envelope = new PaginatedEnvelope<>(null, null, null, searchResult.getTotalRecords(), ImmutableList.of(), null);
            return Response.ok(envelope).build();
        }
    }

    @POST
    public Response createLocation(LocationModel locationModel) {
        Location createdLocation = locationManager.createLocation(representationConverter.to(uriInfo, locationModel));
        return Response.created(UriBuilder.fromUri(uriInfo.getAbsolutePath()).path(createdLocation.getId()).build()).entity(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @GET
    @GZIP
    @Path("/{locationId}")
    public Response getLocation(@PathParam("locationId") String locationId) {

        Location location = locationManager.getLocation(locationId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(location.getLastModified().getTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            return Response.ok(representationConverter.from(uriInfo, location)).tag(etag).build();
        }
    }

    @DELETE
    @Path("/{locationId}")
    public Response deleteLocation(@PathParam("locationId") String locationId) {
        locationManager.deleteLocation(locationId);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @GET
    @GZIP
    @Path("/{locationId}/configuration")
    public Response getLocationConfiguration(@PathParam("locationId") String locationId) {
        Location location = locationManager.getLocation(locationId).orElseThrow(NotFoundException::new);
        return Response.ok().entity(location.getLocationConfiguration()).build();
    }

    @PUT
    @PutLocationConfig
    @Path("/{locationId}/configuration")
    public Response updateLocationConfiguration(@PathParam("locationId") String locationId, LocationConfiguration locationConfiguration) {
        Location location = locationManager.getLocation(locationId).orElseThrow(NotFoundException::new);
        locationManager.updateLocationConfiguration(location.getId(), locationConfiguration);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @GET
    @GZIP
    @Path("/templates")
    public Response getLocationTemplates() {
        ImmutableSet<Template> templates = locationManager.getLocationTemplates();
        ImmutableSet.Builder<TemplateModel> templateModels = ImmutableSet.builder();
        templates.forEach(templateModel -> templateModels.add(new TemplateModel(templateModel.getType(), templateModel.getPropertyDefinitions())));
        return Response.ok().entity(templateModels.build()).build();
    }

    @GET
    @GZIP
    @Path("/templates/{type}")
    public Response getLocationTemplate(@PathParam("type") String type) {
        Template template = locationManager.getLocationTemplate(type);
        return Response.ok().entity(new TemplateModel(type, template.getPropertyDefinitions())).build();
    }
}
