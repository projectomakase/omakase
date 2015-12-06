/*
 * #%L
 * omakase-tool-manifest
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
package org.projectomakase.omakase.worker.tool.manifest;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskOutput;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
@RunWith(CdiTestRunner.class)
public class ManifestToolTest {

    private static final Logger LOGGER = Logger.getLogger(ManifestToolTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Inject
    ManifestTool manifestTool;

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
    public void shouldParseAndTransferManifest() throws Exception {
        File manifest = temporaryFolder.newFile("master.m3u8");
        FileOutputStream fileOutputStream = new FileOutputStream(manifest);
        ByteStreams.copy(ManifestToolTest.class.getResourceAsStream("/master.m3u8"), fileOutputStream);
        String manifestMd5 = Files.hash(manifest, Hashing.md5()).toString();

        File destinationDir = temporaryFolder.newFolder();
        File destination = new File(destinationDir, "temp.txt");

        TaskConfiguration configuration = new ManifestTransferTaskConfiguration(manifest.toURI(), destination.toURI(), ImmutableList.of("MD5"));
        manifestTool.execute(new Task("a", "MANIFEST_TRANSFER", "testing", configuration));

        assertThat(destination.length()).isEqualTo(manifest.length());
        ToolCallback callback = callbacks.get(0);
        assertThat(callback.getTaskStatusUpdate().getStatus()).isEqualTo(TaskStatus.COMPLETED);
        ManifestTransferTaskOutput output = (ManifestTransferTaskOutput) callback.getTaskStatusUpdate().getOutput().get();
        assertThat(output.getSize()).isEqualTo(manifest.length());
        assertThat(output.getHashes().get(0).getValue()).isEqualTo(manifestMd5);
        assertThat(output.getFiles()).isEmpty();
        assertThat(output.getManifests()).hasSize(7);
    }

    public void observeToolContainerCallback(@Observes ToolCallback callback) {
        LOGGER.info("Received tool callback " + callback);
        callbacks.add(callback);
    }
}