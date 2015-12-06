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
package org.projectomakase.omakase.worker.tool.manifest.hls;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.task.providers.manifest.Manifest;
import org.projectomakase.omakase.worker.tool.ToolException;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParserResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class HLSManifestParserTest {

    private HLSManifestParser hlsManifestParser;

    @Before
    public void setUp() throws Exception {
        hlsManifestParser = new HLSManifestParser();
    }

    @Test
    public void shouldParseMasterPlaylist() throws Exception {
        ManifestParserResult result = getManifestParserResult("master.m3u8");
        Assertions.assertThat(result.getFiles()).isEmpty();

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("alternate_audio_aac_sinewave/prog_index.m3u8", "audio, group: bipbop_audio, language: eng, name: BipBop Audio 2, default: false");
        builder.put("subtitles/eng/prog_index.m3u8", "subtitles, group: subs, language: en, name: English, default: true");
        builder.put("subtitles/eng_forced/prog_index.m3u8", "subtitles, group: subs, language: en, name: English (Forced), default: false");
        builder.put("gear1/iframe_index.m3u8", "i-frames, bandwidth: 28451");
        builder.put("gear2/iframe_index.m3u8", "i-frames, bandwidth: 181534");
        builder.put("gear1/prog_index.m3u8", "stream, bandwidth: 263851, resolution: 416x234");
        builder.put("gear2/prog_index.m3u8", "stream, bandwidth: 577610, resolution: 640x360");

        List<Manifest> expectedResults =
                builder.build().entrySet().stream().map(entry -> new Manifest(Throwables.returnableInstance(() -> new URI(entry.getKey())), entry.getValue())).collect(
                        ImmutableListCollector.toImmutableList());

        Assertions.assertThat(result.getManifests()).hasSize(7).usingFieldByFieldElementComparator().containsAll(expectedResults);
    }

    @Test
    public void shouldParseMediaPlaylistSegmented() throws Exception {
        ManifestParserResult result = getManifestParserResult("media.m3u8");
        Assertions.assertThat(result.getManifests()).isEmpty();
        Assertions.assertThat(result.getFiles()).hasSize(15);
    }

    @Test
    public void shouldFailToParseMediaPlaylistByteRanges() throws Exception {
        assertThatThrownBy(() -> getManifestParserResult("media-byterange.m3u8")).isExactlyInstanceOf(ToolException.class).hasMessage("HLS byte-ranges are not supported");
    }

    @Test
    public void shouldParseMediaPlaylistIFrames() throws Exception {
        ManifestParserResult result = getManifestParserResult("media-iframe.m3u8");
        Assertions.assertThat(result.getManifests()).isEmpty();
        Assertions.assertThat(result.getFiles()).hasSize(0);
    }

    private ManifestParserResult getManifestParserResult(String manifestName) throws URISyntaxException {
        URI uri = HLSManifestParserTest.class.getResource("/" + manifestName).toURI();
        return hlsManifestParser.parse(uri, HLSManifestParserTest.class.getResourceAsStream("/" + manifestName));
    }
}