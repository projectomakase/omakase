/*
 * #%L
 * omakase-tool-s3
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
package org.projectomakase.omakase.worker.tool.s3;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.s3.S3Client;
import org.projectomakase.omakase.commons.aws.s3.S3Part;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskConfiguration;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskOutput;
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
public class S3UploadToolTest {

    private static final Logger LOGGER = Logger.getLogger(S3UploadToolTest.class);

    private static List<ToolCallback> callbacks = new ArrayList<>();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Inject
    S3UploadTool tool;

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
    public void shouldUploadToS3() throws Exception {
        tool.s3Client = mock(S3Client.class);

        File source = temporaryFolder.newFile();
        Files.write("This is a test", source, Charsets.UTF_8);
        String sourceMd5 = Files.hash(source, Hashing.md5()).toString();

        URI destination = new URI("s3://access:secret@dev.s3-us-west-1.amazonaws.com/object-one?partNumber=1&uploadId=1234");
        S3Upload s3Upload = AWSClients.s3UploadFromURI(destination);
        ImmutableList.Builder<AWSUploadPart> uploadPartBuilder = ImmutableList.builder();
        uploadPartBuilder.add(new AWSUploadPart(0, 0, source.length(), "c72bed74619a0238b707f13fd6ee8bec14cc7779caca89248350ebc1ac9725ec",
                "ec9364f600b5e47c533056a548fda46affb964198244fe9d08540ad3884aa89f"));
        S3UploadTaskConfiguration configuration = new S3UploadTaskConfiguration(source.toURI(), destination, 4194304, uploadPartBuilder.build(), ImmutableList.of("MD5"));

        // emulate the glacier client reading the stream in order for the tool to generate the correct MD5
        doAnswer(invocation -> {
            InputStream inputStream = (InputStream) invocation.getArguments()[3];
            ByteStreams.copy(inputStream, ByteStreams.nullOutputStream());
            return new S3Part(1, "abc");
        }).when(tool.s3Client).uploadMultipartPart(any(), any(), anyInt(), any());

        Task task = new Task("a", "S3_UPLOAD", "test", configuration);
        tool.execute(task);

        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100, new S3UploadTaskOutput(Collections.singletonList(new S3Part(1, "abc")), Collections.singletonList(new Hash("MD5", sourceMd5))));
    }

    private void validateToolCallback(ToolCallback callback, String expectedTaskId, TaskStatus expectedTaskStatus, int expectedPercentComplete, S3UploadTaskOutput expectedOutput) {
        assertThat(callback.getTaskId()).isEqualTo(expectedTaskId);
        TaskStatusUpdate taskStatusUpdate = callback.getTaskStatusUpdate();
        assertThat(taskStatusUpdate.getStatus()).isEqualTo(expectedTaskStatus);
        assertThat(taskStatusUpdate.getPercentageComplete()).isEqualTo(expectedPercentComplete);
        if (expectedOutput != null) {
            S3UploadTaskOutput output = (S3UploadTaskOutput) callback.getTaskStatusUpdate().getOutput().get();
            assertThat(output.getParts().get(0)).isEqualToComparingFieldByField(expectedOutput.getParts().get(0));
            assertThat(output.getHashes().get(0)).isEqualToComparingFieldByField(expectedOutput.getHashes().get(0));
        } else {
            assertThat(taskStatusUpdate.getOutput().isPresent()).isFalse();
        }
    }
}