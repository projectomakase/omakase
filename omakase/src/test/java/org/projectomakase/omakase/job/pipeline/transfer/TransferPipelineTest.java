/*
 * #%L
 * omakase
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
package org.projectomakase.omakase.job.pipeline.transfer;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.job.configuration.ManifestType;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class TransferPipelineTest {

    @Test
    public void shouldReturnTrueIfTransferHasFiles() throws Exception {
        TransferFile transferFile = TransferFile.builder().destinationRepositoryFileId("123").originalFilename("test.txt").source(new URI("file:/test")).destination(new URI("file:/test2")).build();
        Transfer transfer = new Transfer(ImmutableList.of(TransferFileGroup.builder().transferFiles(ImmutableList.of(transferFile)).build()));
        assertThat(TransferPipeline.doesTransferHaveFiles(transfer)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfTransferHasNoFiles() throws Exception {
        Transfer transfer = new Transfer(ImmutableList.of(TransferFileGroup.builder().build()));
        assertThat(TransferPipeline.doesTransferHaveFiles(transfer)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfTransferHasNoGroups() throws Exception {
        Transfer transfer = new Transfer(ImmutableList.of());
        assertThat(TransferPipeline.doesTransferHaveFiles(transfer)).isFalse();
    }

    @Test
    public void shouldGetAbsoluteSourceUriFromAbsoluteUri() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        URI absoluteUri = new URI("file:/test2/master.m3u8");
        assertThat(TransferPipeline.getAbsoluteSourceUri(parent, absoluteUri)).isEqualTo(absoluteUri);
    }

    @Test
    public void shouldGetAbsoluteSourceUriFromRootUri() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        URI rootUri = new URI("/test2/media.m3u8");
        assertThat(TransferPipeline.getAbsoluteSourceUri(parent, rootUri)).isEqualTo(new URI("file:/test2/media.m3u8"));
    }

    @Test
    public void shouldGetAbsoluteSourceUriFromRelativeUri() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        URI relativeUri = new URI("another/media.m3u8");
        assertThat(TransferPipeline.getAbsoluteSourceUri(parent, relativeUri)).isEqualTo(new URI("file:/test/another/media.m3u8"));
    }

    @Test
    public void shouldGetOriginalFilePathForAbsoluteUriWithDifferentRoot() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        ManifestTransfer manifestTransfer = createManifestTransfer(parent);
        URI uri = new URI("file:/test2/playlist.m3u8");
        assertThat(TransferPipeline.getOriginalFilepath(manifestTransfer, parent, uri)).isEqualTo("test2/playlist.m3u8");
    }

    @Test
    public void shouldGetOriginalFilePathForAbsoluteUriWithSameRoot() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        ManifestTransfer manifestTransfer = createManifestTransfer(parent);
        URI uri = new URI("file:/test/playlist.m3u8");
        assertThat(TransferPipeline.getOriginalFilepath(manifestTransfer, parent, uri)).isEqualTo("playlist.m3u8");
    }

    @Test
    public void shouldGetOriginalFilePathForAbsoluteUriThatIsChildOfRoot() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        ManifestTransfer manifestTransfer = createManifestTransfer(parent);
        URI uri = new URI("file:/test/test2/playlist.m3u8");
        assertThat(TransferPipeline.getOriginalFilepath(manifestTransfer, parent, uri)).isEqualTo("test2/playlist.m3u8");
    }

    @Test
    public void shouldGetOriginalFilePathForRelativeRootUri() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        ManifestTransfer manifestTransfer = createManifestTransfer(parent);
        URI uri = new URI("/test2/playlist.m3u8");
        assertThat(TransferPipeline.getOriginalFilepath(manifestTransfer, parent, uri)).isEqualTo("test2/playlist.m3u8");
    }

    @Test
    public void shouldGetOriginalFilePathForRelativeUri() throws Exception {
        ManifestTransferFile parent = createMasterManifestTransferFile();
        ManifestTransfer manifestTransfer = createManifestTransfer(parent);
        URI uri = new URI("test2/playlist.m3u8");
        assertThat(TransferPipeline.getOriginalFilepath(manifestTransfer, parent, uri)).isEqualTo("test2/playlist.m3u8");
    }

    @Test
    public void shouldGetOriginalFilePathForNestedRelativeUri() throws Exception {
        ManifestTransferFile parent = ManifestTransferFile.builder().source(new URI("file:/test/test2/playlist.m3u8")).originalFilepath("test2/playlist.m3u8").build();
        ManifestTransfer manifestTransfer = createManifestTransfer(parent);
        URI uri = new URI("stream/file0.ts");
        assertThat(TransferPipeline.getOriginalFilepath(manifestTransfer, parent, uri)).isEqualTo("test2/stream/file0.ts");
    }

    private ManifestTransfer createManifestTransfer(ManifestTransferFile manifestTransferFile) throws Exception {
        Path rootPath = Optional.ofNullable(Paths.get("/test/master.m3u8").getParent()).orElse(Paths.get("/"));
        return new ManifestTransfer(rootPath, ManifestType.HLS, ImmutableList.of(manifestTransferFile));
    }

    private ManifestTransferFile createMasterManifestTransferFile() throws Exception {
        return ManifestTransferFile.builder().source(new URI("file:/test/master.m3u8")).originalFilepath("master.m3u8").build();
    }
}