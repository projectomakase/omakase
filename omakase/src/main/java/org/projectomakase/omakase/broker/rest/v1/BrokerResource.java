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
import org.projectomakase.omakase.broker.rest.v1.converter.WorkerQuerySearchConverter;
import org.projectomakase.omakase.broker.rest.v1.model.WorkerModel;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.task.Tasks;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.converter.v1.MessageQuerySearchConverter;
import org.projectomakase.omakase.rest.model.v1.MessageModel;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

/**
 * Broker JAX-RS Resource implementation. Exposes REST API operations for managing and delegating work to brokers.
 *
 * @author Richard Lucas
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class BrokerResource {

    @Inject
    BrokerManager brokerManager;
    @Inject
    RepresentationConverter<WorkerModel, Worker> workerRepresentationConverter;
    @Inject
    WorkerQuerySearchConverter workerQuerySearchConverter;
    @Inject
    RepresentationConverter<MessageModel, Message> messageRepresentationConverter;
    @Inject
    MessageQuerySearchConverter messageQuerySearchConverter;
    @Inject
    Tasks tasks;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    @Path("/broker/workers")
    public WorkersResource getWorkersResource() {
        return new WorkersResource(brokerManager, workerRepresentationConverter, workerQuerySearchConverter, uriInfo);
    }

    @Path("/broker/workers/{workerId}")
    public WorkerResource getWorkerResource() {
        return new WorkerResource(brokerManager, workerRepresentationConverter, uriInfo, request);
    }

    @Path("/broker/workers/{workerId}/messages")
    public WorkerMessagesResource getWorkerMessagesResource() {
        return new WorkerMessagesResource(brokerManager, messageRepresentationConverter, messageQuerySearchConverter, uriInfo);
    }

    @Path("/broker/workers/{workerId}/tasks")
    public WorkerTasksResource getWorkerTasksResource() {
        return  new WorkerTasksResource(brokerManager);
    }

    @Path("/broker/workers/{workerId}/tasks/{taskId}")
    public WorkerTaskResource getWorkerTaskResource() {
        return  new WorkerTaskResource(brokerManager, tasks);
    }


}
