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
package org.projectomakase.omakase.worker.tool.protocol.provider.sftp;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;
import org.projectomakase.omakase.worker.tool.protocol.provider.file.FileProtocolHandler;
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

public class SftpProtocolHandlerTest {

    private static final String CONTENTS = "omakase sftp protocol test";


    private SftpProtocolHandler sftpProtocolHandler;
    private TestSftpServer testSftpServer;
    private long testFileSize;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() throws Exception {
        File home = temporaryFolder.newFolder();
        File dir = new File(home, "dir");
        assertThat(dir.mkdir()).isTrue();
        File testFile = new File(dir, "sample.txt");
        Files.write(CONTENTS, testFile, Charsets.UTF_8);
        testFileSize = testFile.length();
        testSftpServer = new TestSftpServer();
        testSftpServer.start(home);

        sftpProtocolHandler = new SftpProtocolHandler();
    }

    @After
    public void after() throws Exception {
        sftpProtocolHandler.close();
        // Give the client time to disconnect
        Thread.sleep(100);
        testSftpServer.stop();
    }

    @Test
    public void shouldOpenStream() throws Exception {
        URI sourceUri = new URI("sftp://user:password@localhost:2222/dir/sample.txt");
        InputStreamReader reader = null;
        sftpProtocolHandler.init(sourceUri);
        try (InputStream inputStream = sftpProtocolHandler.openStream()) {
            assertThat(inputStream).isNotNull();
            reader = new InputStreamReader(inputStream, Charsets.UTF_8);
            assertThat(CharStreams.toString(reader)).isEqualTo(CONTENTS);
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    @Test
    public void shouldGetContentLength() throws Exception {
        sftpProtocolHandler.init(new URI("sftp://user:password@localhost:2222/dir/sample.txt"));
        assertThat(sftpProtocolHandler.getContentLength()).isEqualTo(testFileSize);
    }

    @Test
    public void shouldOpenStreamAndGetContentLength() throws Exception {
        URI sourceUri = new URI("sftp://user:password@localhost:2222/dir/sample.txt");
        InputStreamReader reader = null;
        sftpProtocolHandler.init(sourceUri);
        assertThat(sftpProtocolHandler.getContentLength()).isEqualTo(testFileSize);
        try (InputStream inputStream = sftpProtocolHandler.openStream()) {
            assertThat(inputStream).isNotNull();
            reader = new InputStreamReader(inputStream, Charsets.UTF_8);
            assertThat(CharStreams.toString(reader)).isEqualTo(CONTENTS);
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    @Test
    public void shouldFailToGetContentLengthFileDoesNotExist() throws Exception {
        try {
            sftpProtocolHandler.init(new URI("sftp://user:password@localhost:2222/dir/sample2.txt"));
            sftpProtocolHandler.getContentLength();
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("Failed to get length of sftp://user:password@localhost:2222/dir/sample2.txt");
        }
    }

    @Test
    public void shouldCopyToPassiveMode() throws Exception {
        File file = temporaryFolder.newFile();
        Files.write("This is a test", file, Charsets.UTF_8);
        URI destinationUri = new URI("sftp://user:password@localhost:2222/dir/sample2.txt");

        try (FileProtocolHandler sourceProtocolHandler = new FileProtocolHandler(); SftpProtocolHandler destSftpProtocolHandler = new SftpProtocolHandler()) {
            sourceProtocolHandler.init(file.toURI());
            destSftpProtocolHandler.init(destinationUri);

            try (InputStream inputStream = sourceProtocolHandler.openStream()) {
                destSftpProtocolHandler.copyTo(inputStream, file.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destSftpProtocolHandler.openStream()) {
                assertThat(inputStream).isNotNull();
                reader = new InputStreamReader(inputStream, Charsets.UTF_8);
                assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
            } finally {
                Closeables.closeQuietly(reader);
            }
        }
    }

    @Test
    public void shouldCopyToActiveMode() throws Exception {
        File file = temporaryFolder.newFile();
        Files.write("This is a test", file, Charsets.UTF_8);
        URI destinationUri = new URI("sftp://user:password@localhost:2222/dir/sample2.txt?passive=false");

        try (FileProtocolHandler sourceProtocolHandler = new FileProtocolHandler(); SftpProtocolHandler destSftpProtocolHandler = new SftpProtocolHandler()) {
            sourceProtocolHandler.init(file.toURI());
            destSftpProtocolHandler.init(destinationUri);

            try (InputStream inputStream = sourceProtocolHandler.openStream()) {
                destSftpProtocolHandler.copyTo(inputStream, file.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destSftpProtocolHandler.openStream()) {
                assertThat(inputStream).isNotNull();
                reader = new InputStreamReader(inputStream, Charsets.UTF_8);
                assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
            } finally {
                Closeables.closeQuietly(reader);
            }
        }
    }

    @Test
    public void shouldCopyToDirectoryThatDoesNotExist() throws Exception {
        File file = temporaryFolder.newFile();
        Files.write("This is a test", file, Charsets.UTF_8);
        URI destinationUri = new URI("sftp://user:password@localhost:2222/dir/test1/test2/sample2.txt");

        try (FileProtocolHandler sourceProtocolHandler = new FileProtocolHandler(); SftpProtocolHandler destSftpProtocolHandler = new SftpProtocolHandler()) {
            sourceProtocolHandler.init(file.toURI());
            destSftpProtocolHandler.init(destinationUri);

            try (InputStream inputStream = sourceProtocolHandler.openStream()) {
                destSftpProtocolHandler.copyTo(inputStream, file.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destSftpProtocolHandler.openStream()) {
                assertThat(inputStream).isNotNull();
                reader = new InputStreamReader(inputStream, Charsets.UTF_8);
                assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
            } finally {
                Closeables.closeQuietly(reader);
            }
        }
    }

    @Test
    public void shouldFailToInitProtocolInvalidUriScheme() throws Exception {
        try {
            sftpProtocolHandler.init(new URI("bad://test"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("bad is not supported by this protocol handler");
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerNoUserInfo() throws Exception {
        try {
            sftpProtocolHandler.init(new URI("sftp://localhost:2222/dir/sample.txt"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("URI is missing user info");
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerInvalidCredentials() throws Exception {
        try {
            sftpProtocolHandler.init(new URI("sftp://bad@localhost:2222/dir/sample.txt"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("URI credentials are invalid");
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerMissingPath() throws Exception {
        try {
            sftpProtocolHandler.init(new URI("sftp://user:password@localhost:2222"));
            sftpProtocolHandler.getContentLength();
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("URI is missing required path");
        }
    }


}