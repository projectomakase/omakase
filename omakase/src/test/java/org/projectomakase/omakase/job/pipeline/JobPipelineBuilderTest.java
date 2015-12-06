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
package org.projectomakase.omakase.job.pipeline;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.assertions.PipelineAssert;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.ExportJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.configuration.ReplicationJobConfiguration;
import org.projectomakase.omakase.job.pipeline.transfer.ExportPrepareStage;
import org.projectomakase.omakase.job.pipeline.transfer.IngestPrepareStage;
import org.projectomakase.omakase.job.pipeline.transfer.ManifestParsingStage;
import org.projectomakase.omakase.job.pipeline.transfer.MultipartPrepareStage;
import org.projectomakase.omakase.job.pipeline.transfer.ReplicationPrepareStage;
import org.projectomakase.omakase.job.pipeline.transfer.RestoreStage;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFailureStage;
import org.projectomakase.omakase.job.pipeline.transfer.TransferStage;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.pipeline.Pipeline;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class JobPipelineBuilderTest {

    private JobPipelineBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new JobPipelineBuilder();
        builder.contentManager = mock(ContentManager.class);
        builder.repositoryManager = mock(RepositoryManager.class);
        builder.locationManager = mock(LocationManager.class);
        MultipartUploadInfoProvider multipartUploadInfoProvider = new MultipartUploadInfoProvider();
        multipartUploadInfoProvider.glacierPartSize = 1048576;
        multipartUploadInfoProvider.s3PartSize = 5242880;
        builder.multipartUploadInfoProvider = multipartUploadInfoProvider;
    }

    @Test
    public void shouldGetIngestPipeline() throws Exception {
        Job job = newJob(JobType.INGEST);
        job.setId("test");
        job.setJobConfiguration(getIngestJobConfiguration("a.txt", false));
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload(anyString());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(IngestPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetIngestPipelineWithMultipartPrepareStage() throws Exception {
        Job job = newJob(JobType.INGEST);
        job.setId("test");
        job.setJobConfiguration(getIngestJobConfiguration("a.txt", false));
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(true).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload(anyString());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(IngestPrepareStage.class, MultipartPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetIngestPipelineWithManifestParsingStaging() throws Exception {
        Job job = newJob(JobType.INGEST);
        job.setId("test");
        job.setJobConfiguration(getIngestJobConfiguration("a.mpd", true));
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload(anyString());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(IngestPrepareStage.class, ManifestParsingStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetIngestPipelineWithMultipartPrepareStageAndManifestParsingStaging() throws Exception {
        Job job = newJob(JobType.INGEST);
        job.setId("test");
        job.setJobConfiguration(getIngestJobConfiguration("a.mpd", true));
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(true).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload(anyString());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(IngestPrepareStage.class, ManifestParsingStage.class, MultipartPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetExportPipeline() throws Exception {
        Job job = newJob(JobType.EXPORT);
        job.setId("test");
        job.setJobConfiguration(getExportJobConfiguration());
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(new URI("file:/test/a.txt")).when(builder.locationManager).expandLocationUri(any());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(ExportPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetExportPipelineWithRestoreStage() throws Exception {
        Job job = newJob(JobType.EXPORT);
        job.setId("test");
        job.setJobConfiguration(getExportJobConfiguration());
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(true).when(builder.repositoryManager).doesRepositoryRequireRestore(anyString());
        doReturn(new URI("file:/test/a.txt")).when(builder.locationManager).expandLocationUri(any());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(ExportPrepareStage.class, RestoreStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetExportPipelineWithMultipartPrepareStage() throws Exception {
        Job job = newJob(JobType.EXPORT);
        job.setId("test");
        job.setJobConfiguration(getExportJobConfiguration());
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireRestore(anyString());
        doReturn(new URI("s3:/test/a.txt")).when(builder.locationManager).expandLocationUri(any());
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(ExportPrepareStage.class, MultipartPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetReplicationPipeline() throws Exception {
        Job job = newJob(JobType.REPLICATION);
        job.setId("test");
        job.setJobConfiguration(getReplicationJobConfiguration());
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireRestore("123");
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload("ABC");
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(ReplicationPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetReplicationPipelineWithRestoreStage() throws Exception {
        Job job = newJob(JobType.REPLICATION);
        job.setId("test");
        job.setJobConfiguration(getReplicationJobConfiguration());
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(true).when(builder.repositoryManager).doesRepositoryRequireRestore("123");
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload("ABC");
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(ReplicationPrepareStage.class, RestoreStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }

    @Test
    public void shouldGetReplicationPipelineWithMultipartPrepareStage() throws Exception {
        Job job = newJob(JobType.REPLICATION);
        job.setId("test");
        job.setJobConfiguration(getReplicationJobConfiguration());
        doReturn(Optional.of(new Variant())).when(builder.contentManager).getVariant(anyString());
        doReturn(false).when(builder.repositoryManager).doesRepositoryRequireRestore("123");
        doReturn(true).when(builder.repositoryManager).doesRepositoryRequireMultipartUpload("ABC");
        new PipelineAssert(builder.build(job, "A"))
                .hasObjectId("test")
                .hasObject("jobs")
                .hasCallbackListenerId("A")
                .hasPipelineStages(ReplicationPrepareStage.class, MultipartPrepareStage.class, TransferStage.class)
                .hasPipelineFailureStage(TransferFailureStage.class);
    }


    @Test
    public void shouldGetDeletePipeline() throws Exception {
        Job job = newJob(JobType.DELETE);
        assertThat(builder.build(job, "A")).isEqualToComparingFieldByField(new Pipeline("test", "jobs", "A", ImmutableList.of(DeleteStage.class), null));
    }

    private Job newJob(JobType jobType) {
        return Job.Builder.build(job -> {
            job.setId("test");
            job.setJobType(jobType);
        });
    }

    private IngestJobConfiguration getIngestJobConfiguration(String uri, boolean isManifest) {
        return IngestJobConfiguration.Builder.build(config -> {
            config.setRepositories(ImmutableList.of("123"));
            config.setIngestJobFiles(Collections.singletonList(IngestJobFile.Builder.build(ingestJobFile -> {
                ingestJobFile.setUri(uri);
                ingestJobFile.setIsManifest(isManifest);
            })));
        });
    }

    private ExportJobConfiguration getExportJobConfiguration() {
        return ExportJobConfiguration.Builder.build(config -> {
            config.setRepositories(ImmutableList.of("123"));
            config.setVariant("a");
            config.setLocations(ImmutableList.of("123"));
        });
    }

    private ReplicationJobConfiguration getReplicationJobConfiguration() {
        return ReplicationJobConfiguration.Builder.build(config -> {
            config.setSourceRepositories(ImmutableList.of("123"));
            config.setDestinationRepositories(ImmutableList.of("ABC"));
            config.setVariant("a");
        });
    }
}