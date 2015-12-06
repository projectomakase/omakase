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
package org.projectomakase.omakase.sns;

import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.job.task.queue.TaskStatusQueue;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.restore.RestoreTaskOutput;
import org.apache.deltaspike.core.api.provider.DependentProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Richard Lucas
 */
public class ArchiveRetrievalNotificationConsumerTest {

    private static final String SUCCESS = "{\"Action\":\"ArchiveRetrieval\",\"" +
            "ArchiveId\":\"lNoJo-OHrmhIM-fTw6W6gwvldfWlHsxowT7CRV_k5TSfdQcqLYyXFFXA-TMYE8c-mOUcdQlmu5VvWegQZNYUrtOwKcGM279KziutQI3ZunY8wsgdWgrxHZiYSY0P4d7Lou9WiPQX_w\",\"" +
            "ArchiveSHA256TreeHash\":\"05c734c3f16b23358bb49c959d1420edac9f28ee844bf9b0580754c0f540acd8\",\"ArchiveSizeInBytes\":1049350,\"Completed\":true,\"" +
            "CompletionDate\":\"2015-04-28T20:50:12.529Z\",\"CreationDate\":\"2015-04-28T16:30:09.259Z\",\"InventoryRetrievalParameters\":null,\"InventorySizeInBytes\":null," +
            "\"JobDescription\":\"test-task-id\",\"JobId\":\"ty4__gb6pead4EbLwh11g_MiSofKnWM0ZmAtc-KSfKWdwqQdd_kxodB6YhMNssCA6q4iWYy9aNjJb0y0lMmIy8Z3YHPa\",\"RetrievalByteRange\":\"0-1049349\"," +
            "\"SHA256TreeHash\":\"05c734c3f16b23358bb49c959d1420edac9f28ee844bf9b0580754c0f540acd8\",\"SNSTopic\":\"arn:aws:sns:us-west-1:015638829286:omakase\"," +
            "\"StatusCode\":\"Succeeded\",\"StatusMessage\":\"Succeeded\",\"VaultARN\":\"arn:aws:glacier:us-west-1:015638829286:vaults/dev\"}";

    private static final String FAILED = "{\"Action\":\"ArchiveRetrieval\",\"" +
            "ArchiveId\":\"lNoJo-OHrmhIM-fTw6W6gwvldfWlHsxowT7CRV_k5TSfdQcqLYyXFFXA-TMYE8c-mOUcdQlmu5VvWegQZNYUrtOwKcGM279KziutQI3ZunY8wsgdWgrxHZiYSY0P4d7Lou9WiPQX_w\",\"" +
            "ArchiveSHA256TreeHash\":\"05c734c3f16b23358bb49c959d1420edac9f28ee844bf9b0580754c0f540acd8\",\"ArchiveSizeInBytes\":1049350,\"Completed\":true,\"" +
            "CompletionDate\":\"2015-04-28T20:50:12.529Z\",\"CreationDate\":\"2015-04-28T16:30:09.259Z\",\"InventoryRetrievalParameters\":null,\"InventorySizeInBytes\":null," +
            "\"JobDescription\":\"test-task-id\",\"JobId\":\"ty4__gb6pead4EbLwh11g_MiSofKnWM0ZmAtc-KSfKWdwqQdd_kxodB6YhMNssCA6q4iWYy9aNjJb0y0lMmIy8Z3YHPa\",\"RetrievalByteRange\":\"0-1049349\"," +
            "\"SHA256TreeHash\":\"05c734c3f16b23358bb49c959d1420edac9f28ee844bf9b0580754c0f540acd8\",\"SNSTopic\":\"arn:aws:sns:us-west-1:015638829286:omakase\"," +
            "\"StatusCode\":\"Failed\",\"StatusMessage\":\"Failed\",\"VaultARN\":\"arn:aws:glacier:us-west-1:015638829286:vaults/dev\"}";

    private ArchiveRetrievalNotificationConsumer consumer;
    private TaskStatusQueue taskStatusQueue;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        taskStatusQueue = mock(TaskStatusQueue.class);
        DependentProvider<TaskStatusQueue> taskStatusQueueProvider = mock(DependentProvider.class);
        doReturn(taskStatusQueue).when(taskStatusQueueProvider).get();
        consumer = new ArchiveRetrievalNotificationConsumer(taskStatusQueueProvider);
    }

    @Test
    public void shouldConsumeSuccessNotification() throws Exception {
        ArgumentCaptor<String> capturedTaskId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TaskStatusUpdate> capturedTaskStatusUpdate = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        consumer.accept(parseMessage(SUCCESS));
        verify(taskStatusQueue).add(capturedTaskId.capture(), capturedTaskStatusUpdate.capture());
        assertThat(capturedTaskId.getValue()).isEqualTo("test-task-id");
        assertThat(capturedTaskStatusUpdate.getValue()).isEqualToIgnoringGivenFields(new TaskStatusUpdate(TaskStatus.COMPLETED, "Succeeded", 100), "output");
        assertThat(capturedTaskStatusUpdate.getValue().getOutput()).usingFieldByFieldValueComparator()
                .contains(new RestoreTaskOutput(Throwables.returnableInstance(() -> new URI("ty4__gb6pead4EbLwh11g_MiSofKnWM0ZmAtc-KSfKWdwqQdd_kxodB6YhMNssCA6q4iWYy9aNjJb0y0lMmIy8Z3YHPa"))));
    }

    @Test
    public void shouldConsumeFailureNotification() throws Exception {
        ArgumentCaptor<String> capturedTaskId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TaskStatusUpdate> capturedTaskStatusUpdate = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        consumer.accept(parseMessage(FAILED));
        verify(taskStatusQueue).add(capturedTaskId.capture(), capturedTaskStatusUpdate.capture());
        assertThat(capturedTaskId.getValue()).isEqualTo("test-task-id");
        assertThat(capturedTaskStatusUpdate.getValue()).isEqualToIgnoringNullFields(new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed", 0));
    }

    private JsonObject parseMessage(String message) {
        try (StringReader stringReader = new StringReader(message); JsonReader jsonReader = Json.createReader(stringReader)) {
            return jsonReader.readObject();
        }
    }
}