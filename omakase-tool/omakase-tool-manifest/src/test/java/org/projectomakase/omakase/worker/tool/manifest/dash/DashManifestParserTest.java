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
package org.projectomakase.omakase.worker.tool.manifest.dash;

import org.projectomakase.omakase.task.providers.manifest.ManifestFile;
import org.projectomakase.omakase.worker.tool.manifest.ManifestFileBuilder;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParserResult;
import org.projectomakase.omakase.worker.tool.manifest.assertions.ManifestFileAssert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class DashManifestParserTest {

    private DashManifestParser dashManifestParser;

    @Before
    public void setUp() throws Exception {
        dashManifestParser = new DashManifestParser();
        ManifestFileBuilder manifestFileBuilder = mock(ManifestFileBuilder.class);
        doAnswer(invocation -> new ManifestFile((URI) invocation.getArguments()[0], 1024)).when(manifestFileBuilder).build(any());
        dashManifestParser.manifestFileBuilder = manifestFileBuilder;
    }

    @Test
    public void shouldParse() throws Exception {
        ManifestParserResult manifestParserResult = getManifestParserResult("dash.mpd");
        assertThat(manifestParserResult.getManifests()).isEmpty();

        new ManifestFileAssert(manifestParserResult.getFiles().get(0)).hasURI(new URI("test/feelings_vp9-20130806-171.webm")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(1)).hasURI(new URI("test/a.mp4")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(2)).hasURI(new URI("test/feelings_vp9-20130806-172.webm")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(3)).hasURI(new URI("test/1.idx")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(4)).hasURI(new URI("test/b.mp4")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(5)).hasURI(new URI("test/test2/2.idx")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(6)).hasURI(new URI("test/test2/hello.mp4")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(7)).hasURI(new URI("test/test2/feelings_vp9-20130806-242.webm")).hasSize(1024);
        new ManifestFileAssert(manifestParserResult.getFiles().get(8)).hasURI(new URI("test/test2/feelings_vp9-20130806-243.webm")).hasSize(1024);
    }

    private ManifestParserResult getManifestParserResult(String manifestName) throws URISyntaxException {
        URI uri = DashManifestParserTest.class.getResource("/" + manifestName).toURI();
        return dashManifestParser.parse(uri, DashManifestParserTest.class.getResourceAsStream("/" + manifestName));
    }

}