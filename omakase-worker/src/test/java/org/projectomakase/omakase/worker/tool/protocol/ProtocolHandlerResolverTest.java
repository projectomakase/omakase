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
package org.projectomakase.omakase.worker.tool.protocol;

import org.projectomakase.omakase.worker.tool.protocol.provider.file.FileProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.provider.ftp.FtpProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.provider.glacier.GlacierProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.provider.http.HttpProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.provider.http.HttpsProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.provider.s3.S3ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.provider.sftp.SftpProtocolHandler;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(CdiTestRunner.class)
public class ProtocolHandlerResolverTest {

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldGetFileProtocolHandler() throws Exception {
        URI uri = temporaryFolder.newFile().toURI();
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(FileProtocolHandler.class);
    }

    @Test
    public void shouldGetHttpProtocolHandler() throws Exception {
        URI uri = new URI("http://localhost/test");
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(HttpProtocolHandler.class);
    }

    @Test
    public void shouldGetHttpsProtocolHandler() throws Exception {
        URI uri = new URI("https://localhost/test");
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(HttpsProtocolHandler.class);
    }

    @Test
    public void shouldGetFtpProtocolHandler() throws Exception {
        URI uri = new URI("ftp://localhost/test");
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(FtpProtocolHandler.class);
    }

    @Test
    public void shouldGetSFtpProtocolHandler() throws Exception {
        URI uri = new URI("sftp://localhost/test");
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(SftpProtocolHandler.class);
    }

    @Test
    public void shouldGetS3ProtocolHandler() throws Exception {
        URI uri = new URI("s3://localhost/test");
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(S3ProtocolHandler.class);
    }

    @Test
    public void shouldGetGlacierProtocolHandler() throws Exception {
        URI uri = new URI("glacier://localhost/test");
        assertThat(protocolHandlerResolver.getProtocolHandler(uri)).isInstanceOf(GlacierProtocolHandler.class);
    }
}