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

import com.comcast.viper.hlsparserj.tags.UnparsedTag;
import com.comcast.viper.hlsparserj.tags.master.IFrameStreamInf;
import com.comcast.viper.hlsparserj.tags.master.Media;
import com.comcast.viper.hlsparserj.tags.master.StreamInf;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class HLSStreamNameTest {

    @Test
    public void shouldGetStreamName() throws Exception {
        StreamInf streamInf = new StreamInf();
        UnparsedTag tag = new UnparsedTag();
        tag.setAttributes(ImmutableMap.of("BANDWIDTH", "1000", "RESOLUTION", "1x1"));
        streamInf.setTag(tag);
        assertThat(HLSStreamName.getStreamName(streamInf)).isEqualTo("stream, bandwidth: 1000, resolution: 1x1");
    }

    @Test
    public void shouldGetStreamNameWithNoResolution() throws Exception {
        StreamInf streamInf = new StreamInf();
        UnparsedTag tag = new UnparsedTag();
        tag.setAttributes(ImmutableMap.of("BANDWIDTH", "1000"));
        streamInf.setTag(tag);
        assertThat(HLSStreamName.getStreamName(streamInf)).isEqualTo("stream, bandwidth: 1000");
    }

    @Test
    public void shouldGetIframeStreamName() throws Exception {
        IFrameStreamInf iFrameStreamInf = new IFrameStreamInf();
        UnparsedTag tag = new UnparsedTag();
        tag.setAttributes(ImmutableMap.of("BANDWIDTH", "1000"));
        iFrameStreamInf.setTag(tag);
        assertThat(HLSStreamName.getIFrameStreamName(iFrameStreamInf)).isEqualTo("i-frames, bandwidth: 1000");
    }

    public void shouldGetMediaStreamName() throws Exception {
        Media media = new Media();
        UnparsedTag tag = new UnparsedTag();
        tag.setAttributes(ImmutableMap.of("GROUP-ID", "a", "LANGUAGE", "eng", "NAME", "test", "DEFAULT", "false"));
        media.setTag(tag);
        assertThat(HLSStreamName.getMediaStreamName(media)).isEqualTo("media, group: a, language: eng, name: test, default: false");
    }
}