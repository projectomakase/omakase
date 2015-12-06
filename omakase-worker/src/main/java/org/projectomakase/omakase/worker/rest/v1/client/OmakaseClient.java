/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker.rest.v1.client;

import com.google.common.collect.ImmutableList;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskInstanceLoader;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.worker.Omakase;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.ToolInfo;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Omakase REST API V1 Client.
 *
 * @author Richard Lucas
 */
@Named
@ApplicationScoped
public class OmakaseClient {

    private static final Logger LOGGER = Logger.getLogger(OmakaseClient.class);

    private static final String BASE_API_PATH = "/omakase/api";
    private static final String WORKERS_API_PATH = BASE_API_PATH + "/broker/workers";
    private static final String WORKER_API_PATH = BASE_API_PATH + "/broker/workers/%s";
    private static final String TASKS_API_PATH = BASE_API_PATH + "/broker/workers/%s/tasks";
    private static final String STATUS_API_PATH = BASE_API_PATH + "/broker/workers/%s/tasks/%s/status";

    private static final MediaType APPLICATION_CONSUME_TYPE = new MediaType("application", "consume-tasks+json");
    private static final MediaType APPLICATION_PRODUCE_TYPE = new MediaType("application", "produce-status+json");

    private static final String CAPACITY_JSON_TEMPLATE = "{\"type\":\"%s\",\"availability\":%d}";
    private static final String WORKER_JSON_TEMPLATE = "{\"name\":\"%s\",\"external_ids\":[]}";

    @Inject
    @ConfigProperty(name = "omakase.url")
    String omakaseUrl;
    @Inject
    @ConfigProperty(name = "omakase.user")
    String omakaseUser;
    @Inject
    @ConfigProperty(name = "omakase.password")
    String omakasePassword;
    @Inject
    @Omakase
    HttpClient httpClient;

    // we store the worker id assigned at recitation on the client as it is required when making calls to Omakase.
    String workerId;

    private ResteasyClient resteasyClient;
    private TasksBuilder tasksBuilder;

    @PostConstruct
    public void init() {
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        this.resteasyClient = new ResteasyClientBuilder().httpEngine(engine).register(new BasicAuthenticationFilter()).build();
        this.tasksBuilder = new TasksBuilder();
    }


    /**
     * Consumes the next set of available tasks for the given list of available tools via the Omakase Tool REST API.
     *
     * @param toolInfos
     *         a set of {@link ToolInfo} instances that have available capacity
     * @return the next available message or an empty payload if there are no messages to consume.
     */
    public List<Task> consumeToolTasks(Set<ToolInfo> toolInfos) {
        String resource = String.format(TASKS_API_PATH, workerId);
        String json = "[" + toolInfos.stream().map(toolInfo -> String.format(CAPACITY_JSON_TEMPLATE, toolInfo.getName(), toolInfo.getAvailableCapacity())).collect(Collectors.joining(",")) + "]";

        return invoke(getInvocationBuilder(resource).accept(MediaType.APPLICATION_JSON_TYPE).build("POST", entity(json, APPLICATION_CONSUME_TYPE))).map(this::getTasksFromResponse)
                .orElse(ImmutableList.of());
    }

    /**
     * Posts the specified task status update to Omakase via the Task REST API.
     *
     * @param toolCallback
     *         the tool callback
     */
    public void produceToolTaskStatusUpdate(ToolCallback toolCallback) {
        String resource = String.format(STATUS_API_PATH, workerId, toolCallback.getTaskId());
        invoke(getInvocationBuilder(resource).accept(MediaType.APPLICATION_JSON_TYPE).build("POST", entity(toolCallback.getTaskStatusUpdate().toJson(), APPLICATION_PRODUCE_TYPE)))
                .ifPresent(response -> response.readEntity(String.class));
    }

    /**
     * Posts the specified worker information to register with the Omakase Worker REST API.
     *
     * @param workerName
     *         the worker's name
     * @return the worker id assigned to the worker when it registers.
     * @throws OmakaseRuntimeException
     *         if registration fails.
     */
    public String registerWorker(String workerName) {
        String location = invoke(getInvocationBuilder(WORKERS_API_PATH).accept(MediaType.APPLICATION_JSON_TYPE).build("POST", Entity.json(String.format(WORKER_JSON_TEMPLATE, workerName))))
                .map(response -> {
                    String loc = response.getHeaderString("Location");
                    // required to ensure connection closed
                    response.readEntity(String.class);
                    return loc;
                }).orElseThrow(() -> new OmakaseRuntimeException("Registration failed, no worker id was assigned"));
        workerId = location.substring(location.lastIndexOf("/") + 1, location.length());
        return workerId;
    }

    /**
     * Deletes the specified worker information to unregister from the Omakase Worker REST API.
     */
    public void unregisterWorker() {
        invoke(getInvocationBuilder(String.format(WORKER_API_PATH, workerId)).accept(MediaType.APPLICATION_JSON_TYPE).build("DELETE")).ifPresent(response -> response.readEntity(String.class));
        workerId = null;
    }

    private List<Task> getTasksFromResponse(Response response) {
        if (response.getStatus() == 200) {
            return tasksBuilder.fromJson(response.readEntity(String.class));
        } else {
            LOGGER.error("Failed to retrieve tasks from Omakase, Reason: " + response.getStatusInfo().getReasonPhrase());
            return ImmutableList.of();
        }
    }

    private Invocation.Builder getInvocationBuilder(String resource) {
        WebTarget webTarget = resteasyClient.target(omakaseUrl).path(resource);
        return webTarget.request();
    }

    private static Optional<Response> invoke(Invocation invocation) {
        try {
            return Optional.of(invocation.invoke());
        } catch (ProcessingException e) {
            LOGGER.error("Failed to invoke Omakase REST API request");
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(e.getMessage(), e);
            }
            return Optional.empty();
        }
    }

    private static Entity<String> entity(String entity, MediaType mediaType) {
        return Entity.entity(entity, mediaType);
    }

    class BasicAuthenticationFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            final String basicAuthentication = getBasicAuthentication();
            headers.add("Authorization", basicAuthentication);

        }

        private String getBasicAuthentication() {
            final String token = omakaseUser + ":" + omakasePassword;
            try {
                return "Basic " + DatatypeConverter.printBase64Binary(token.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException("Cannot encode with UTF-8", ex);
            }
        }
    }

    private class TasksBuilder {

        public List<Task> fromJson(String json) {
            try (StringReader stringReader = new StringReader(json); JsonReader jsonReader = Json.createReader(stringReader)) {
                JsonArray jsonArray = jsonReader.readArray();
                return jsonArray.stream().map(jsonValue -> (JsonObject) jsonValue).map(this::fromJsonObject).collect(ImmutableListCollector.toImmutableList());
            }
        }

        private Task fromJsonObject(JsonObject jsonObject) {
            String type = jsonObject.getString("type");
            TaskConfiguration taskConfiguration = TaskInstanceLoader.loadTaskConfigurationInstance(type);
            taskConfiguration.fromJson(jsonObject.getJsonObject("configuration").toString());
            return new Task(jsonObject.getString("id"), jsonObject.getString("type"), jsonObject.getString("description"), taskConfiguration);
        }
    }
}
