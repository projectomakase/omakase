/*
 * #%L
 * omakase-tool-hash
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
package org.projectomakase.omakase.worker.tool.hash;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.file.FileGenerator;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.hash.HashInput;
import org.projectomakase.omakase.task.providers.hash.HashTaskConfiguration;
import org.projectomakase.omakase.task.providers.hash.HashTaskOutput;
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
import java.util.stream.IntStream;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
@RunWith(CdiTestRunner.class)
public class HashToolTest {

    private static final Logger LOGGER = Logger.getLogger(HashToolTest.class);
    private static final long FOUR_MB = 1024 * 1024 * 4;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Inject
    HashTool hashTool;

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
    public void shouldCreateTreeHash() throws Exception {
        File source = FileGenerator.generate(temporaryFolder.getRoot(), FOUR_MB);
        Task task = new Task("a", "HASH", "test", new HashTaskConfiguration(source.toURI(), ImmutableList.of(new HashInput(Hashes.TREE_HASH))));
        hashTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        HashTaskOutput taskOutput = new HashTaskOutput(ImmutableList.of(new Hash(Hashes.TREE_HASH, "aaf178963dba5142d81e6feda72bfaf1c7064df2276672ec9295326367444ce4", 0, source.length())));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100, taskOutput);
    }


    @Test
    public void shouldCreateSHA256ForEachInput() throws Exception {
        File source = FileGenerator.generate(temporaryFolder.getRoot(), FOUR_MB);
        Task task =
                new Task("a", "HASH", "test", new HashTaskConfiguration(source.toURI(), ImmutableList.of(new HashInput(Hashes.SHA256, 0, 1048576), new HashInput(Hashes.SHA256, 1048576, 2097152))));
        hashTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        HashTaskOutput taskOutput = new HashTaskOutput(ImmutableList.of(new Hash(Hashes.SHA256, "5bfea26ced5ed2670589c3e3549413dbe74e1ca23af296374ea9307ac5e79101", 0, 1048576),
                new Hash(Hashes.SHA256, "73f4b2725be9db345d9b3aa14345cecf9fd74e469ebc8160c5def7fb6a1140fa", 1048576, 2097152)));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100, taskOutput);
    }

    @Test
    public void shouldCreateSHA256AndMD5AndTreeHash() throws Exception {
        File source = FileGenerator.generate(temporaryFolder.getRoot(), FOUR_MB);
        long contentLength = source.length();
        Task task =
                new Task("a", "HASH", "test", new HashTaskConfiguration(source.toURI(), ImmutableList.of(new HashInput(Hashes.SHA256), new HashInput(Hashes.MD5), new HashInput(Hashes.TREE_HASH))));
        hashTool.execute(task);
        await().until(() -> assertThat(callbacks).hasSize(1));
        HashTaskOutput taskOutput = new HashTaskOutput(ImmutableList.of(new Hash(Hashes.SHA256, "593aae576b3cfc6ad474600343c26f822144087f712572b08fa56c3beabf7f18", 0, contentLength),
                new Hash(Hashes.MD5, "e2325cb8e030bff536aa278ef6b699ef", 0, contentLength),
                new Hash(Hashes.TREE_HASH, "aaf178963dba5142d81e6feda72bfaf1c7064df2276672ec9295326367444ce4", 0, contentLength)));
        validateToolCallback(callbacks.get(0), "a", TaskStatus.COMPLETED, 100, taskOutput);
    }

    public void observeToolContainerCallback(@Observes ToolCallback callback) {
        LOGGER.info("Received tool task status update callback " + callback);
        callbacks.add(callback);
    }

    private void validateToolCallback(ToolCallback callback, String expectedTaskId, TaskStatus expectedTaskStatus, int expectedPercentComplete, HashTaskOutput expectedOutput) {
        assertThat(callback.getTaskId()).isEqualTo(expectedTaskId);
        TaskStatusUpdate taskStatusUpdate = callback.getTaskStatusUpdate();
        assertThat(taskStatusUpdate.getStatus()).isEqualTo(expectedTaskStatus);
        assertThat(taskStatusUpdate.getPercentageComplete()).isEqualTo(expectedPercentComplete);
        if (expectedOutput != null) {
            HashTaskOutput output = (HashTaskOutput) callback.getTaskStatusUpdate().getOutput().get();
            IntStream.range(0, output.getHashes().size()).forEach(idx -> assertThat(output.getHashes().get(idx)).isEqualToComparingFieldByField(expectedOutput.getHashes().get(idx)));
        } else {
            assertThat(taskStatusUpdate.getOutput().isPresent()).isFalse();
        }
    }

}