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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.http.HttpClientFactory;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.ToolInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class OmakaseClientTest {

    private static final String TASKS_JSON_TEMPLATE = "[{\"id\":\"%s\",\"type\":\"%s\",\"description\":\"%s\",\"configuration\":%s}]";

    private OmakaseClient omakaseClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void before() {
        omakaseClient = new OmakaseClient();
        omakaseClient.omakaseUrl = "http://localhost:8089";
        omakaseClient.omakaseUser = "admin";
        omakaseClient.omakasePassword = "password";
        omakaseClient.httpClient = HttpClientFactory.pooledConnectionHttpClient(100, 30000, 30000);
        omakaseClient.init();
    }

    @After
    public void after() {
        omakaseClient.httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void shouldRegister() throws Exception {
        ResponseDefinitionBuilder responseDefinitionBuilder =
                aResponse().withStatus(201).withHeader("Content-Type", "application/json").withHeader("Location", "/broker/workers/100").withBody("{\"status\": \"OK\"}");
        addStubFor("/omakase/api/broker/workers", responseDefinitionBuilder);

        assertThat(omakaseClient.registerWorker("test")).isEqualTo("100");
    }


    @Test
    public void shouldConsumeToolTasks() throws Exception {
        TransferTaskConfiguration transferTaskConfiguration = new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(new URI("file://tmp/source.mov"), new URI("file://test"))),
                                                                                            Collections.singletonList("MD5"));
        String tasksJson = String.format(TASKS_JSON_TEMPLATE, "96fc0a25_a352_451f_8e04_469d4009e108", "TRANSFER", "test", transferTaskConfiguration.toJson());
        ResponseDefinitionBuilder responseDefinitionBuilder = aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(tasksJson);
        addStubFor("/omakase/api/broker/workers/1112/tasks", responseDefinitionBuilder);
        omakaseClient.workerId = "1112";
        List<Task> tasks = omakaseClient.consumeToolTasks(ImmutableSet.of(new ToolInfo("TRANSFER", 1, 1)));

        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getId()).isEqualTo("96fc0a25_a352_451f_8e04_469d4009e108");
        assertThat(task.getType()).isEqualTo("TRANSFER");
        assertThat(task.getDescription()).isEqualTo("test");
    }

    @Test
    public void shouldSendTaskStatusUpdateWithATaskOutput() throws Exception {
        ResponseDefinitionBuilder responseDefinitionBuilder = aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{\"status\": \"OK\"}");
        addStubFor("/omakase/api/broker/workers/1102/tasks/1111111/status", responseDefinitionBuilder);
        omakaseClient.workerId = "1110";
        TransferTaskOutput taskOutput = new TransferTaskOutput(ImmutableList.of(new ContentInfo(new URI("file:/test.txt"), 1024, Collections.singletonList(new Hash("MD5", "123")))));
        TaskStatusUpdate statusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "success", 100, taskOutput);
        omakaseClient.produceToolTaskStatusUpdate(new ToolCallback("TRANSFER", "1111111", statusUpdate));
        verifyRequest("/omakase/api/broker/workers/1110/tasks/1111111/status", "application/produce-status+json", "application/json", statusUpdate.toJson());

    }

    @Test
    public void shouldSendTaskStatusUpdateWithOutATaskOutput() throws Exception {
        ResponseDefinitionBuilder responseDefinitionBuilder = aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{\"status\": \"OK\"}");
        addStubFor("/omakase/api/broker/workers/1102/tasks/1111111/status", responseDefinitionBuilder);
        omakaseClient.workerId = "1110";
        TaskStatusUpdate statusUpdate = new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "failed", 0);
        omakaseClient.produceToolTaskStatusUpdate(new ToolCallback("TRANSFER", "1111111", statusUpdate));
        verifyRequest("/omakase/api/broker/workers/1110/tasks/1111111/status", "application/produce-status+json", "application/json", statusUpdate.toJson());
    }

    private void addStubFor(String location, ResponseDefinitionBuilder responseDefinitionBuilder) {
        stubFor(post(urlEqualTo(location)).willReturn(responseDefinitionBuilder));
    }

    private void verifyRequest(String location, String expectedRequestBody) {
        verifyRequest(location, "application/json", "application/json", expectedRequestBody);
    }

    private void verifyRequest(String location, String contentType, String accept, String expectedRequestBody) {
        verify(postRequestedFor(urlEqualTo(location)).withHeader("Content-Type", equalTo(contentType)).withHeader("Accept", equalTo(accept)).withRequestBody(equalTo(expectedRequestBody)));
    }
}