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
import org.projectomakase.omakase.job.task.Tasks;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * JAX-RS Subresource for /broker/worker/{workerId}/tasks/{taskId}
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class WorkerTaskResource {

    private final BrokerManager brokerManager;
    private final Tasks tasks;

    public WorkerTaskResource(BrokerManager brokerManager, Tasks tasks) {
        this.brokerManager = brokerManager;
        this.tasks = tasks;
    }

    @Path("status")
    @POST
    @Consumes({"application/produce-status+json", "application/produce-status.v1+json"})
    public Response produceTaskStatus(@PathParam("workerId") String workerId, @PathParam("taskId") String taskId, String taskStatusUpdateJson) {
        brokerManager.handleTaskStatusUpdateFromWorker(workerId, taskId, tasks.taskStatusUpdateFromJson(taskId, taskStatusUpdateJson));
        return Response.ok(new ResponseStatusModel(ResponseStatusValue.OK)).build();
    }


}
