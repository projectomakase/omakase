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

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.worker.tool.manifest.assertions.ManifestFileAssert;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerResolver;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class ManifestFileBuilderTest {

    private ManifestFileBuilder manifestFileBuilder;

    @Before
    public void setUp() throws Exception {
        manifestFileBuilder = new ManifestFileBuilder();
        ProtocolHandlerResolver protocolHandlerResolver = mock(ProtocolHandlerResolver.class);
        ProtocolHandler protocolHandler = mock(ProtocolHandler.class);
        doReturn(1024L).when(protocolHandler).getContentLength();
        doReturn(protocolHandler).when(protocolHandlerResolver).getProtocolHandler(any());
        manifestFileBuilder.protocolHandlerResolver = protocolHandlerResolver;
    }

    @Test
    public void shouldBuildManifestFile() throws Exception {
        manifestFileBuilder.init(new URI("file:/test/a.xml"));
        new ManifestFileAssert(manifestFileBuilder.build(new URI("test2/a.mp4"))).hasURI(new URI("test2/a.mp4")).hasSize(1024);
    }

    @Test
    public void shouldThrowExceptionIfNotInitialized() throws Exception {
        assertThatThrownBy(() -> manifestFileBuilder.build(new URI("test2/a.mp4")))
                .isExactlyInstanceOf(OmakaseRuntimeException.class)
                .hasMessage("ManifestFileBuilder has not been initialized");
    }
}