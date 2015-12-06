/*
 * #%L
 * omakase-tool-transfer
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
package org.projectomakase.omakase.worker.tool.transfer;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(CdiTestRunner.class)
public class TransferToolTest {

    private static final Logger LOGGER = Logger.getLogger(TransferToolTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Inject
    TransferTool transferTool;

    private static List<ToolCallback> callbacks = new ArrayList<>();

    @Before
    public void before() {
        callbacks.clear();
    }

    @After
    public void after() {
        callbacks.clear();
    }

    @Test
    public void shouldTransferSingleIOInstruction() throws Exception {
        File source = temporaryFolder.newFile();
        Files.write("This is a test", source, Charsets.UTF_8);
        String sourceMd5 = Files.hash(source, Hashing.md5()).toString();

        File destinationDir = temporaryFolder.newFolder();
        File destination = new File(destinationDir, "temp.txt");

        Task task = newTransferTask(getTransferTaskConfiguration(source.toURI(), destination.toURI()));
        transferTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100, expectedTransferTaskOutput(source, sourceMd5));

        validateFileWritenToDest(destination);
    }

    private void validateFileWritenToDest(File destination) throws IOException {
        InputStreamReader reader = null;
        try (FileInputStream inputStream = new FileInputStream(destination)) {
            assertThat(inputStream).isNotNull();
            reader = new InputStreamReader(inputStream, Charsets.UTF_8);
            assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    @Test
    public void shouldTransferMultipleIOInstructions() throws Exception {

        // IO Instruction one
        File source = temporaryFolder.newFile();
        Files.write("This is a test", source, Charsets.UTF_8);
        File destinationDir = temporaryFolder.newFolder();
        File destination = new File(destinationDir, "temp.txt");
        IOInstruction ioInstructionOne = new IOInstruction(source.toURI(), destination.toURI());

        // IO Instruction two
        File sourceTwo = temporaryFolder.newFile();
        Files.write("This is a test", sourceTwo, Charsets.UTF_8);
        File destinationDirTwo = temporaryFolder.newFolder();
        File destinationTwo = new File(destinationDirTwo, "temp.txt");
        IOInstruction ioInstructionTwo = new IOInstruction(sourceTwo.toURI(), destinationTwo.toURI());

        Task task = newTransferTask(new TransferTaskConfiguration(ImmutableList.of(ioInstructionOne, ioInstructionTwo), Collections.singletonList("MD5")));
        transferTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));

        validateFileWritenToDest(destination);
        validateFileWritenToDest(destinationTwo);
    }

    @Test
    public void shouldFailToTransferFromFileToFile() throws Exception {
        File source = new File("badFile.txt");

        File destinationDir = temporaryFolder.newFolder();
        File destination = new File(destinationDir, "temp.txt");

        Task task = newTransferTask(getTransferTaskConfiguration(source.toURI(), destination.toURI()));
        transferTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.FAILED_DIRTY, 0, null);
        assertThat(callbacks.get(0).getTaskStatusUpdate().getMessage()).startsWith("Failed to transfer file. Reason:");
    }

    @Test
    public void shouldFailToTransferWhenPropertiesDoesNotContainSource() throws Exception {
        Task task = newTransferTask(getTransferTaskConfiguration(null, new URI("file://test")));
        transferTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.FAILED_DIRTY, 0, null);
        assertThat(callbacks.get(0).getTaskStatusUpdate().getMessage()).isEqualTo("Failed to transfer file. Reason: source uri can not be null");
    }

    @Test
    public void shouldFailToTransferWhenPropertiesDoesNotContainDestination() throws Exception {
        Task task = newTransferTask(getTransferTaskConfiguration(new URI("file://test"), null));
        transferTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.FAILED_DIRTY, 0, null);
        assertThat(callbacks.get(0).getTaskStatusUpdate().getMessage()).isEqualTo("Failed to transfer file. Reason: destination uri can not be null");
    }

    public void observeToolContainerCallback(@Observes ToolCallback callback) {
        LOGGER.info("Received tool callback " + callback);
        callbacks.add(callback);
    }

    private Task newTransferTask(TransferTaskConfiguration configuration) {
        return new Task("a", "TRANSFER", "testing", configuration);
    }

    private TransferTaskConfiguration getTransferTaskConfiguration(URI source, URI destination) {
        return new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(source, destination)), Collections.singletonList("MD5"));
    }

    private TransferTaskOutput expectedTransferTaskOutput(File source, String sourceMd5) {
        return new TransferTaskOutput(ImmutableList.of(new ContentInfo(source.toURI(), source.length(), Collections.singletonList(new Hash("MD5", sourceMd5)))));
    }

    private void validateToolCallback(ToolCallback callback, String expectedTaskId, TaskStatus expectedTaskStatus, int expectedPercentComplete, TransferTaskOutput expectedOutput) {
        assertThat(callback.getTaskId()).isEqualTo(expectedTaskId);
        TaskStatusUpdate taskStatusUpdate = callback.getTaskStatusUpdate();
        assertThat(taskStatusUpdate.getStatus()).isEqualTo(expectedTaskStatus);
        assertThat(taskStatusUpdate.getPercentageComplete()).isEqualTo(expectedPercentComplete);
        if (expectedOutput != null) {
            TransferTaskOutput output = (TransferTaskOutput) callback.getTaskStatusUpdate().getOutput().get();

//            assertThat(output.getSize()).isEqualTo(expectedOutput.getSize());
//            assertThat(output.getHashes().get(0)).isEqualToComparingFieldByField(expectedOutput.getHashes().get(0));
        } else {
            assertThat(taskStatusUpdate.getOutput().isPresent()).isFalse();
        }
    }
}