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
package org.projectomakase.omakase.broker.rest.v1;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.broker.BrokerManager;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.broker.rest.v1.converter.WorkerQuerySearchConverter;
import org.projectomakase.omakase.broker.rest.v1.model.WorkerModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.PaginatedEnvelope;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.projectomakase.omakase.rest.pagination.v1.PaginationLinks;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.spi.LinkHeaders;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

/**
 * JAX-RS Subresource for /broker/workers
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class WorkersResource {

    private final BrokerManager brokerManager;
    private final RepresentationConverter<WorkerModel, Worker> workerRepresentationConverter;
    private final WorkerQuerySearchConverter querySearchConverter;
    private final UriInfo uriInfo;

    public WorkersResource(BrokerManager brokerManager, RepresentationConverter<WorkerModel, Worker> workerRepresentationConverter, WorkerQuerySearchConverter querySearchConverter, UriInfo uriInfo) {
        this.brokerManager = brokerManager;
        this.workerRepresentationConverter = workerRepresentationConverter;
        this.querySearchConverter = querySearchConverter;
        this.uriInfo = uriInfo;
    }

    @GET
    @GZIP
    public Response getWorkers(@QueryParam("page") @DefaultValue("1") int page, @QueryParam("per_page") @DefaultValue("10") int perPage,
                               @DefaultValue("false") @QueryParam("only_count") boolean onlyCount) {

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(true);
        Search search = querySearchConverter.from(queryParams);
        SearchResult<Worker> searchResult = brokerManager.findWorkers(search);
        if (!onlyCount) {
            Collection<WorkerModel> jobModels = workerRepresentationConverter.from(uriInfo, searchResult.getRecords());
            PaginationLinks paginationLinks = new PaginationLinks(uriInfo, page, perPage, searchResult.getTotalRecords());
            PaginatedEnvelope<WorkerModel> envelope = new PaginatedEnvelope<>(page, perPage, paginationLinks.getTotalPages(), searchResult.getTotalRecords(), jobModels, paginationLinks.get());
            LinkHeaders linkHeaders = new LinkHeaders();
            paginationLinks.get().forEach((rel, href) -> linkHeaders.addLink(Link.fromUri(href.getHref()).rel(rel).build()));
            return Response.ok(envelope).links(linkHeaders.getLinks().toArray(new Link[linkHeaders.getLinks().size()])).build();
        } else {
            PaginatedEnvelope<WorkerModel> envelope = new PaginatedEnvelope<>(null, null, null, searchResult.getTotalRecords(), ImmutableList.of(), null);
            return Response.ok(envelope).build();
        }
    }

    @POST
    public Response createWorker(WorkerModel workerModel) {
        workerRepresentationConverter.to(uriInfo, workerModel);
        Worker createdWorker = brokerManager.registerWorker(workerRepresentationConverter.to(uriInfo, workerModel));
        return Response.created(UriBuilder.fromUri(uriInfo.getAbsolutePath()).path(createdWorker.getId()).build()).entity(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }
}
