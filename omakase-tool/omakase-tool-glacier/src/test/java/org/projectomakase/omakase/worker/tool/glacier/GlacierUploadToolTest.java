/*
 * #%L
 * omakase-tool-glacier
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
package org.projectomakase.omakase.worker.tool.glacier;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;
import org.projectomakase.omakase.commons.aws.glacier.GlacierUpload;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskConfiguration;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskOutput;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;


/**
 * @author Richard Lucas
 */
@RunWith(CdiTestRunner.class)
public class GlacierUploadToolTest {

    private static final Logger LOGGER = Logger.getLogger(GlacierUploadToolTest.class);

    private static List<ToolCallback> callbacks = new ArrayList<>();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Inject
    GlacierUploadTool tool;

    @Before
    public void before() {
        callbacks.clear();
    }

    @After
    public void after() {
        callbacks.clear();
    }

    public void observeToolContainerCallback(@Observes ToolCallback callback) {
        LOGGER.info("Received tool callback " + callback);
        callbacks.add(callback);
    }

    @Test
    public void shouldUploadToGlacier() throws Exception {
        tool.glacierClient = mock(GlacierClient.class);

        File source = temporaryFolder.newFile();
        Files.write("This is a test", source, Charsets.UTF_8);
        String sourceMd5 = Files.hash(source, Hashing.md5()).toString();

        URI destination = new URI("glacier://test:test@glacier.us-west-1.amazonaws.com/-/vaults/dev/multipart-uploads/1234");
        GlacierUpload glacierUpload = AWSClients.glacierUploadFromURI(destination);
        ImmutableList.Builder<AWSUploadPart> uploadPartBuilder = ImmutableList.builder();
        uploadPartBuilder.add(new AWSUploadPart(0, 0, source.length(), "c72bed74619a0238b707f13fd6ee8bec14cc7779caca89248350ebc1ac9725ec",
                "ec9364f600b5e47c533056a548fda46affb964198244fe9d08540ad3884aa89f"));
        GlacierUploadTaskConfiguration configuration = new GlacierUploadTaskConfiguration(source.toURI(), destination, 4194304, uploadPartBuilder.build(), ImmutableList.of("MD5"));

        // emulate the glacier client reading the stream in order for the tool to generate the correct MD5
        doAnswer(invocation -> {
            InputStream inputStream = (InputStream) invocation.getArguments()[3];
            ByteStreams.copy(inputStream, ByteStreams.nullOutputStream());
            return true;
        }).when(tool.glacierClient).uploadMultipartPart(any(), any(), anyInt(), any());

        Task task = new Task("a", "GLACIER_UPLOAD", "test", configuration);
        tool.execute(task);

        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100, new GlacierUploadTaskOutput(Collections.singletonList(new Hash("MD5", sourceMd5))));
    }

    private void validateToolCallback(ToolCallback callback, String expectedTaskId, TaskStatus expectedTaskStatus, int expectedPercentComplete, GlacierUploadTaskOutput expectedOutput) {
        assertThat(callback.getTaskId()).isEqualTo(expectedTaskId);
        TaskStatusUpdate taskStatusUpdate = callback.getTaskStatusUpdate();
        assertThat(taskStatusUpdate.getStatus()).isEqualTo(expectedTaskStatus);
        assertThat(taskStatusUpdate.getPercentageComplete()).isEqualTo(expectedPercentComplete);
        if (expectedOutput != null) {
            GlacierUploadTaskOutput output = (GlacierUploadTaskOutput) callback.getTaskStatusUpdate().getOutput().get();
            assertThat(output.getHashes().get(0)).isEqualToComparingFieldByField(expectedOutput.getHashes().get(0));
        } else {
            assertThat(taskStatusUpdate.getOutput().isPresent()).isFalse();
        }
    }

}