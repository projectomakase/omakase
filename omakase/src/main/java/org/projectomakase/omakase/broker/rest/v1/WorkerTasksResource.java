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
import org.projectomakase.omakase.broker.Capacity;
import org.projectomakase.omakase.broker.rest.v1.model.CapacityModel;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.task.api.Task;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JAX-RS Subresource for /broker/worker/{workerId}/tasks
 *
 * @author Richard Lucas
 */
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class WorkerTasksResource {

    private static final String JSON_TEMPLATE = "{\"id\":\"%s\",\"type\":\"%s\",\"description\":\"%s\",\"configuration\":%s}";

    private final BrokerManager brokerManager;

    public WorkerTasksResource(BrokerManager brokerManager) {
        this.brokerManager = brokerManager;
    }

    @POST
    @Consumes({"application/consume-tasks+json", "application/consume-tasks.v1+json"})
    public Response consumeTasks(@PathParam("workerId") String workerId, List<CapacityModel> capacity) {
        return Response.ok(getTasksForWorkerJson(workerId, capacity)).build();
    }

    private String getTasksForWorkerJson(String workerId, List<CapacityModel> capacity) {
        ImmutableList<Capacity> caps = capacity.stream().map(cap -> new Capacity(cap.getType(), cap.getAvailability())).collect(ImmutableListCollector.toImmutableList());
        ImmutableList<Task> tasks = brokerManager.getNextAvailableTasksForWorker(workerId, caps);
        return "[" + tasks.stream().map(WorkerTasksResource::taskToJson).collect(Collectors.joining(",")) + "]";
    }

    private static String taskToJson(Task task) {
        return String.format(JSON_TEMPLATE, task.getId(), task.getType(), task.getDescription(), task.getConfiguration().toJson());
    }
}
