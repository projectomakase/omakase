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
package org.projectomakase.omakase.repository.rest.v1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.jcr.Template;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.rest.v1.converter.RepositoryQuerySearchConverter;
import org.projectomakase.omakase.repository.rest.v1.interceptor.PutRepositoryConfig;
import org.projectomakase.omakase.repository.rest.v1.model.RepositoryModel;
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
 * Repository Management JAX-RS Resource implementation. Exposes REST API operations for managing repositories.
 *
 * @author Richard Lucas
 */
@Path("repositories")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class RepositoryResource {

    @Inject
    RepositoryManager repositoryManager;
    @Inject
    RepresentationConverter<RepositoryModel, Repository> representationConverter;
    @Inject
    RepositoryQuerySearchConverter querySearchConverter;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    @GET
    @GZIP
    public Response getRepositories(@QueryParam("page") @DefaultValue("1") int page, @QueryParam("per_page") @DefaultValue("10") int perPage,
                                    @DefaultValue("false") @QueryParam("only_count") boolean onlyCount) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(true);
        Search search = querySearchConverter.from(queryParams);
        SearchResult<Repository> searchResult = repositoryManager.findRepositories(search);
        if (!onlyCount) {
            Collection<RepositoryModel> jobModels = representationConverter.from(uriInfo, searchResult.getRecords());
            PaginationLinks paginationLinks = new PaginationLinks(uriInfo, page, perPage, searchResult.getTotalRecords());
            PaginatedEnvelope<RepositoryModel> envelope =
                    new PaginatedEnvelope<>(page, perPage, paginationLinks.getTotalPages(), searchResult.getTotalRecords(), jobModels, paginationLinks.get());
            LinkHeaders linkHeaders = new LinkHeaders();
            paginationLinks.get().forEach((rel, href) -> linkHeaders.addLink(Link.fromUri(href.getHref()).rel(rel).build()));
            return Response.ok(envelope).links(linkHeaders.getLinks().toArray(new Link[linkHeaders.getLinks().size()])).build();
        } else {
            PaginatedEnvelope<RepositoryModel> envelope = new PaginatedEnvelope<>(null, null, null, searchResult.getTotalRecords(), ImmutableList.of(), null);
            return Response.ok(envelope).build();
        }
    }

    @POST
    public Response createRepository(RepositoryModel repositoryModel) {
        Repository createdRepository = repositoryManager.createRepository(representationConverter.to(uriInfo, repositoryModel));
        return Response.created(UriBuilder.fromUri(uriInfo.getAbsolutePath()).path(createdRepository.getId()).build()).entity(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @GET
    @GZIP
    @Path("/{repositoryId}")
    public Response getRepository(@PathParam("repositoryId") String repositoryId) {

        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(repository.getLastModified().getTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            return Response.ok(representationConverter.from(uriInfo, repository)).tag(etag).build();
        }
    }

    @DELETE
    @Path("/{repositoryId}")
    public Response deleteRepository(@PathParam("repositoryId") String repositoryId) {
        repositoryManager.deleteRepository(repositoryId);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @GET
    @GZIP
    @Path("/{repositoryId}/configuration")
    public Response getRepositoryConfiguration(@PathParam("repositoryId") String repositoryId) {
        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(NotFoundException::new);
        return Response.ok().entity(repository.getRepositoryConfiguration()).build();
    }

    @PUT
    @PutRepositoryConfig
    @Path("/{repositoryId}/configuration")
    public Response updateRepositoryConfiguration(@PathParam("repositoryId") String repositoryId, RepositoryConfiguration repositoryConfiguration) {
        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(NotFoundException::new);
        repositoryManager.updateRepositoryConfiguration(repository.getId(), repositoryConfiguration);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @GET
    @GZIP
    @Path("/templates")
    public Response getRepositoryTemplates() {
        ImmutableSet<Template> templates = repositoryManager.getRepositoryTemplates();
        ImmutableSet.Builder<TemplateModel> repositoryTemplateModels = ImmutableSet.builder();
        templates.forEach(repositoryTemplate -> repositoryTemplateModels.add(new TemplateModel(repositoryTemplate.getType(), repositoryTemplate.getPropertyDefinitions())));
        return Response.ok().entity(repositoryTemplateModels.build()).build();
    }

    @GET
    @GZIP
    @Path("/templates/{type}")
    public Response getRepositoryTemplate(@PathParam("type") String type) {
        Template template = repositoryManager.getRepositoryTemplate(type);
        return Response.ok().entity(new TemplateModel(type, template.getPropertyDefinitions())).build();
    }
}
