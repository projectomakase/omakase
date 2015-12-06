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
package org.projectomakase.omakase.worker.tool.protocol.provider.file;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class FileProtocolHandlerTest {

    private FileProtocolHandler fileProtocolHandler;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        fileProtocolHandler = new FileProtocolHandler();
    }

    @After
    public void after() {
        fileProtocolHandler.close();
    }

    @Test
    public void shouldOpenStream() throws Exception {
        File file = temporaryFolder.newFile();
        Files.write("This is a test", file, Charsets.UTF_8);
        InputStreamReader reader = null;
        fileProtocolHandler.init(file.toURI());
        try (InputStream inputStream = fileProtocolHandler.openStream()) {
            assertThat(inputStream).isNotNull();
            reader = new InputStreamReader(inputStream, Charsets.UTF_8);
            assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    @Test
    public void shouldGetContentLength() throws Exception {
        File file = temporaryFolder.newFile();
        Files.write("This is a test", file, Charsets.UTF_8);
        long expectedSize = java.nio.file.Files.size(file.toPath());
        fileProtocolHandler.init(file.toURI());
        assertThat(fileProtocolHandler.getContentLength()).isEqualTo(expectedSize);
    }

    @Test
    public void shouldCopyTo() throws Exception {
        File sourceFile = temporaryFolder.newFile();
        File destinationFile = temporaryFolder.newFile();
        Files.write("This is a test", sourceFile, Charsets.UTF_8);

        try (FileProtocolHandler destFileProtocolHandler = new FileProtocolHandler(); FileProtocolHandler sourceFileProtocolHandler = new FileProtocolHandler()) {
            sourceFileProtocolHandler.init(sourceFile.toURI());
            destFileProtocolHandler.init(destinationFile.toURI());

            try (InputStream inputStream = sourceFileProtocolHandler.openStream()) {
                destFileProtocolHandler.copyTo(inputStream, sourceFile.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destFileProtocolHandler.openStream()) {
                assertThat(inputStream).isNotNull();
                reader = new InputStreamReader(inputStream, Charsets.UTF_8);
                assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
            } finally {
                Closeables.closeQuietly(reader);
            }
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerInvalidUriScheme() throws Exception {
        try {
            fileProtocolHandler.init(new URI("bad://test"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("bad is not supported by this protocol handler");
        }
    }

}