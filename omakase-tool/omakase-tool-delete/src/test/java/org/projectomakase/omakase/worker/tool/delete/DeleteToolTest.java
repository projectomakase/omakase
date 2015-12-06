/*
 * #%L
 * omakase-tool-delete
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
package org.projectomakase.omakase.worker.tool.delete;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.delete.DeleteTaskConfiguration;
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
import java.util.ArrayList;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
@RunWith(CdiTestRunner.class)
public class DeleteToolTest {

    private static final Logger LOGGER = Logger.getLogger(DeleteToolTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Inject
    DeleteTool deleteTool;

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
    public void shouldDeleteFiles() throws Exception {
        File source1 = temporaryFolder.newFile();
        File source2 = temporaryFolder.newFile();
        Files.write("This is a test", source1, Charsets.UTF_8);
        Files.write("This is a test", source2, Charsets.UTF_8);
        Task taskMessage = new Task("a", "DELETE", "Delete", new DeleteTaskConfiguration(ImmutableList.of(source1.toURI(), source2.toURI())));
        deleteTool.execute(taskMessage);
        await().until(() -> assertThat(callbacks).hasSize(1));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100);
        assertThat(java.nio.file.Files.exists(source1.toPath())).isFalse();
        assertThat(java.nio.file.Files.exists(source2.toPath())).isFalse();
    }

    public void observeToolContainerCallback(@Observes ToolCallback callback) {
        LOGGER.info("Received tool callback " + callback);
        callbacks.add(callback);
    }

    private void validateToolCallback(ToolCallback callback, String expectedTaskId, TaskStatus expectedTaskStatus, int expectedPercentComplete) {
        assertThat(callback.getTaskId()).isEqualTo(expectedTaskId);
        TaskStatusUpdate taskStatusUpdate = callback.getTaskStatusUpdate();
        assertThat(taskStatusUpdate.getStatus()).isEqualTo(expectedTaskStatus);
        assertThat(taskStatusUpdate.getPercentageComplete()).isEqualTo(expectedPercentComplete);
        assertThat(taskStatusUpdate.getOutput().isPresent()).isFalse();
    }

}