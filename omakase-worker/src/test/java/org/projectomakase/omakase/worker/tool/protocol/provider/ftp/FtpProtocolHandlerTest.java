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
package org.projectomakase.omakase.worker.tool.protocol.provider.ftp;

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

public class FtpProtocolHandlerTest {

    private static final String CONTENTS = "omakase ftp protocol test";


    private FtpProtocolHandler ftpProtocolHandler;
    private TestFtpServer testFtpServer;
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
        testFtpServer = new TestFtpServer();
        testFtpServer.start(home);

        ftpProtocolHandler = new FtpProtocolHandler();
    }

    @After
    public void after() throws Exception{
        ftpProtocolHandler.close();
        // Give the client time to disconnect
        Thread.sleep(100);
        testFtpServer.stop();
    }

    @Test
    public void shouldOpenStream() throws Exception {
        URI sourceUri = new URI("ftp://user:password@localhost:2221/dir/sample.txt");
        InputStreamReader reader = null;
        ftpProtocolHandler.init(sourceUri);
        try (InputStream inputStream = ftpProtocolHandler.openStream()) {
            assertThat(inputStream).isNotNull();
            reader = new InputStreamReader(inputStream, Charsets.UTF_8);
            assertThat(CharStreams.toString(reader)).isEqualTo(CONTENTS);
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    @Test
    public void shouldFailToOpenStreamFileDoesNotExist() throws Exception {
        URI sourceUri = new URI("ftp://user:password@localhost:2221/dir/bad.txt");
        ftpProtocolHandler.init(sourceUri);
        try (InputStream ignored = ftpProtocolHandler.openStream()) {
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch(ProtocolHandlerException e) {
            assertThat(e).hasMessage("Failed to open stream for ftp://user:password@localhost:2221/dir/bad.txt. 550 /dir/bad.txt: No such file or directory.");
        }
    }

    @Test
    public void shouldGetContentLength() throws Exception {
        ftpProtocolHandler.init(new URI("ftp://user:password@localhost:2221/dir/sample.txt"));
        assertThat(ftpProtocolHandler.getContentLength()).isEqualTo(testFileSize);
    }

    @Test
    public void shouldOpenStreamAndGetContentLength() throws Exception {
        URI sourceUri = new URI("ftp://user:password@localhost:2221/dir/sample.txt");
        InputStreamReader reader = null;
        ftpProtocolHandler.init(sourceUri);
        assertThat(ftpProtocolHandler.getContentLength()).isEqualTo(testFileSize);
        try (InputStream inputStream = ftpProtocolHandler.openStream()) {
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
            ftpProtocolHandler.init(new URI("ftp://user:password@localhost:2221/dir/sample2.txt"));
            ftpProtocolHandler.getContentLength();
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("Failed to get length of ftp://user:password@localhost:2221/dir/sample2.txt. It does not exist.");
        }
    }

    @Test
    public void shouldCopyToPassiveMode() throws Exception {
        File file = temporaryFolder.newFile();
        Files.write("This is a test", file, Charsets.UTF_8);
        URI destinationUri = new URI("ftp://user:password@localhost:2221/dir/sample2.txt");

        try (FileProtocolHandler sourceProtocolHandler = new FileProtocolHandler(); FtpProtocolHandler destFtpProtocolHandler = new FtpProtocolHandler()) {
            sourceProtocolHandler.init(file.toURI());
            destFtpProtocolHandler.init(destinationUri);

            try (InputStream inputStream = sourceProtocolHandler.openStream()) {
                destFtpProtocolHandler.copyTo(inputStream, file.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destFtpProtocolHandler.openStream()) {
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
        URI destinationUri = new URI("ftp://user:password@localhost:2221/dir/sample2.txt?passive=false");

        try (FileProtocolHandler sourceProtocolHandler = new FileProtocolHandler(); FtpProtocolHandler destFtpProtocolHandler = new FtpProtocolHandler()) {
            sourceProtocolHandler.init(file.toURI());
            destFtpProtocolHandler.init(destinationUri);

            try (InputStream inputStream = sourceProtocolHandler.openStream()) {
                destFtpProtocolHandler.copyTo(inputStream, file.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destFtpProtocolHandler.openStream()) {
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
        URI destinationUri = new URI("ftp://user:password@localhost:2221/dir/test1/test2/sample2.txt");

        try (FileProtocolHandler sourceProtocolHandler = new FileProtocolHandler(); FtpProtocolHandler destFtpProtocolHandler = new FtpProtocolHandler()) {
            sourceProtocolHandler.init(file.toURI());
            destFtpProtocolHandler.init(destinationUri);

            try (InputStream inputStream = sourceProtocolHandler.openStream()) {
                destFtpProtocolHandler.copyTo(inputStream, file.length());
            }

            InputStreamReader reader = null;
            try (InputStream inputStream = destFtpProtocolHandler.openStream()) {
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
            ftpProtocolHandler.init(new URI("bad://test"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("bad is not supported by this protocol handler");
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerNoUserInfo() throws Exception {
        try {
            ftpProtocolHandler.init(new URI("ftp://localhost:2221/dir/sample.txt"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("URI is missing user info");
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerInvalidCredentials() throws Exception {
        try {
            ftpProtocolHandler.init(new URI("ftp://bad@localhost:2221/dir/sample.txt"));
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("URI credentials are invalid");
        }
    }

    @Test
    public void shouldFailToInitProtocolHandlerMissingPath() throws Exception {
        try {
            ftpProtocolHandler.init(new URI("ftp://user:password@localhost:2221"));
            ftpProtocolHandler.getContentLength();
            failBecauseExceptionWasNotThrown(ProtocolHandlerException.class);
        } catch (ProtocolHandlerException e) {
            assertThat(e).hasMessage("URI is missing required path");
        }
    }


}