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

import org.projectomakase.omakase.broker.BrokerManager;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.broker.rest.v1.model.WorkerModel;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.jboss.resteasy.annotations.GZIP;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * JAX-RS Subresource for /broker/worker/{workerId}
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class WorkerResource {

    private final BrokerManager brokerManager;
    private final RepresentationConverter<WorkerModel, Worker> workerRepresentationConverter;
    private final UriInfo uriInfo;
    private final Request request;

    public WorkerResource(BrokerManager brokerManager, RepresentationConverter<WorkerModel, Worker> workerRepresentationConverter, UriInfo uriInfo, Request request) {
        this.brokerManager = brokerManager;
        this.workerRepresentationConverter = workerRepresentationConverter;
        this.uriInfo = uriInfo;
        this.request = request;
    }

    @GET
    @GZIP
    public Response getWorker(@PathParam("workerId") String workerId) {
        Worker worker = brokerManager.getWorker(workerId).orElseThrow(NotFoundException::new);
        EntityTag etag = EntityTagGenerator.entityTagFromLong(worker.getLastModified().getTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            WorkerModel workerModel = workerRepresentationConverter.from(uriInfo, worker);
            return Response.ok(workerModel).tag(etag).build();
        }
    }

    @PUT
    public Response updateWorker(@PathParam("workerId") String workerId, WorkerModel workerModel) {
        brokerManager.updateWorker(workerRepresentationConverter.update(uriInfo, workerModel, brokerManager.getWorker(workerId).orElseThrow(NotFoundException::new)));
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }

    @DELETE
    public Response deleteWorker(@PathParam("workerId") String workerId) {
        brokerManager.unregisterWorker(workerId);
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }
}
