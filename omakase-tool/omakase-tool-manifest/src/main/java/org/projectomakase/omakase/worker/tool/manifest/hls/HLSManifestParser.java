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

import com.comcast.viper.hlsparserj.IPlaylist;
import com.comcast.viper.hlsparserj.MasterPlaylist;
import com.comcast.viper.hlsparserj.MediaPlaylist;
import com.comcast.viper.hlsparserj.PlaylistFactory;
import com.comcast.viper.hlsparserj.PlaylistVersion;
import com.comcast.viper.hlsparserj.tags.media.Segment;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.task.providers.manifest.Manifest;
import org.projectomakase.omakase.task.providers.manifest.ManifestFile;
import org.projectomakase.omakase.worker.tool.ToolException;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParser;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParserResult;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Parses HTTP Live Stream (HLS) playlists.
 *
 * @author Richard Lucas
 */
public class HLSManifestParser implements ManifestParser {

    private static final Logger LOGGER = Logger.getLogger(HLSManifestParser.class);

    @Override
    public ManifestParserResult parse(URI manifestUri, InputStream inputStream) {
        IPlaylist playlist = Throwables.returnableInstance(() -> PlaylistFactory.parsePlaylist(PlaylistVersion.DEFAULT, inputStream));
        if (playlist.isMasterPlaylist()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parsing Master Playlist");
            }
            return new ManifestParserResult(parseMasterPlaylist((MasterPlaylist) playlist), ImmutableList.of());
        } else {
            return new ManifestParserResult(ImmutableList.of(), ImmutableList.copyOf(parseMediaPlaylist((MediaPlaylist) playlist)));
        }
    }

    private static List<Manifest> parseMasterPlaylist(MasterPlaylist masterPlaylist) {
        ImmutableList.Builder<Manifest> listBuilder = ImmutableList.builder();
        // parse alts (audio, video, sub-titles)
        masterPlaylist.getAlternateRenditions().stream().filter(media -> media.getURI() != null)
                .forEach(media -> listBuilder.add(newManifest(media.getURI(), HLSStreamName.getMediaStreamName(media))));
        // parse i-frames
        masterPlaylist.getIFrameStreams().forEach(stream -> listBuilder.add(newManifest(stream.getURI(), HLSStreamName.getIFrameStreamName(stream))));
        // parse streams
        masterPlaylist.getVariantStreams().forEach(stream -> listBuilder.add(newManifest(stream.getURI(), HLSStreamName.getStreamName(stream))));
        return listBuilder.build();
    }

    private static List<ManifestFile> parseMediaPlaylist(MediaPlaylist mediaPlaylist) {
        if (mediaPlaylist.getIFramesOnly()) {
            // I-Frames point to the same file(s) as the segments and do not need to be ingested as they are just meta-data
            return ImmutableList.of();
        } else if (!mediaPlaylist.getByteRanges().isEmpty()) {
            // We do not currently support ingesting manifests that use a single large file with byte ranges.
            throw new ToolException("HLS byte-ranges are not supported");
        } else {
            return mediaPlaylist.getSegments().stream()
                    .map(Segment::getURI)
                    .map(uri -> Throwables.returnableInstance(() -> new ManifestFile(new URI(uri))))
                    .collect(ImmutableListCollector.toImmutableList());
        }
    }

    private static Manifest newManifest(String uri, String description) {
        URI manifestUri = Throwables.returnableInstance(() -> new URI(uri));
        return new Manifest(manifestUri, description);
    }
}
