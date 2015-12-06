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

import com.comcast.viper.hlsparserj.tags.master.IFrameStreamInf;
import com.comcast.viper.hlsparserj.tags.master.Media;
import com.comcast.viper.hlsparserj.tags.master.StreamInf;

import java.util.Optional;

/**
 * Generates the stream name from the stream data.
 *
 * @author Richard Lucas
 */
public final class HLSStreamName {

    private HLSStreamName() {
        // hide default constructor
    }

    public static String getStreamName(StreamInf stream) {
        return "stream" + Optional.ofNullable(stream.getBandwidth()).map(s -> ", bandwidth: " + s).orElse("") + Optional.ofNullable(stream.getResolution()).map(s -> ", resolution: " + s).orElse("");
    }

    public static String getIFrameStreamName(IFrameStreamInf stream) {
        return "i-frames" + Optional.ofNullable(stream.getBandwidth()).map(s -> ", bandwidth: " + s).orElse("");
    }


    public static String getMediaStreamName(Media media) {
        return media.getType().toLowerCase() + Optional.ofNullable(media.getGroupId()).map(s -> ", group: " + s).orElse("") +
                Optional.ofNullable(media.getLanguage()).map(s -> ", language: " + s).orElse("") + Optional.ofNullable(media.getName()).map(s -> ", name: " + s).orElse("") +
                Optional.ofNullable(media.getDefault()).map(s -> ", default: " + s).orElse("");
    }
}
