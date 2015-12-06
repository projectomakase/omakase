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
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantType;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.configuration.ExportJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.ManifestType;
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
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Job pipeline builder.
 *
 * @author Richard Lucas
 */
public class JobPipelineBuilder {

    private static final Logger LOGGER = Logger.getLogger(JobPipelineBuilder.class);

    private static final String WORKFLOW_OBJECT_NAME = "jobs";
    private static final List<Class<? extends PipelineStage>> DELETE_STAGES = ImmutableList.of(DeleteStage.class);

    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    LocationManager locationManager;
    @Inject
    MultipartUploadInfoProvider multipartUploadInfoProvider;

    /**
     * Builds a {@link Pipeline} for the specified job.
     *
     * @param job
     *         the job
     * @param callbackListenerId
     *         the callback listener id the pipeline should use when sending callbacks
     * @return a {@link Pipeline}.
     */
    public Pipeline build(Job job, String callbackListenerId) {
        Pipeline pipeline;
        switch (job.getJobType()) {
            case INGEST:
                pipeline = getIngestPipeline(job, callbackListenerId);
                break;
            case EXPORT:
                pipeline = getExportPipeline(job, callbackListenerId);
                break;
            case REPLICATION:
                pipeline = getReplicationPipeline(job, callbackListenerId);
                break;
            case DELETE:
                pipeline = new Pipeline(job.getId(), WORKFLOW_OBJECT_NAME, callbackListenerId, DELETE_STAGES, null);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Job Type " + job.getJobType());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created Job " + pipeline);
        }
        return pipeline;
    }

    private Pipeline getIngestPipeline(Job job, String callbackListenerId) {
        IngestJobConfiguration ingestJobConfiguration = (IngestJobConfiguration) job.getJobConfiguration();
        return new Pipeline(job.getId(), WORKFLOW_OBJECT_NAME, callbackListenerId, getIngestStages(ingestJobConfiguration), TransferFailureStage.class);
    }

    private List<Class<? extends PipelineStage>> getIngestStages(IngestJobConfiguration ingestJobConfiguration) {
        ImmutableList.Builder<Class<? extends PipelineStage>> stageBuilder = ImmutableList.builder();
        stageBuilder.add(IngestPrepareStage.class);

        Optional<ManifestType> manifestType = ingestJobConfiguration.getManifestType();
        manifestType.ifPresent(type -> stageBuilder.add(ManifestParsingStage.class));
        if (repositoryManager.doesRepositoryRequireMultipartUpload(ingestJobConfiguration.getRepositories().get(0))) {
            if (!manifestType.isPresent()) {
                stageBuilder.add(MultipartPrepareStage.class);
            }
            manifestType.filter(type -> !ManifestType.HLS.equals(type)).ifPresent(type -> stageBuilder.add(MultipartPrepareStage.class));
        }
        stageBuilder.add(TransferStage.class);
        return stageBuilder.build();
    }

    private Pipeline getExportPipeline(Job job, String callbackListenerId) {
        ExportJobConfiguration exportJobConfiguration = (ExportJobConfiguration) job.getJobConfiguration();
        Variant variant =
                contentManager.getVariant(exportJobConfiguration.getVariant()).orElseThrow(() -> new OmakaseRuntimeException("Unable to retrieve variant " + exportJobConfiguration.getVariant()));
        String repositoryId = exportJobConfiguration.getRepositories().get(0);

        ImmutableList.Builder<Class<? extends PipelineStage>> stageBuilder = ImmutableList.builder();
        stageBuilder.add(ExportPrepareStage.class);

        if (repositoryManager.doesRepositoryRequireRestore(repositoryId)) {
            stageBuilder.add(RestoreStage.class);
        }
        String destinationLocation = exportJobConfiguration.getLocations().stream().findFirst().get();

        if (multipartUploadInfoProvider.get(locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI(destinationLocation)))).isPresent() &&
                !VariantType.HLS_MANIFEST.equals(variant.getType())) {
            stageBuilder.add(MultipartPrepareStage.class);
        }
        stageBuilder.add(TransferStage.class);

        return new Pipeline(job.getId(), WORKFLOW_OBJECT_NAME, callbackListenerId, stageBuilder.build(), TransferFailureStage.class);
    }

    private Pipeline getReplicationPipeline(Job job, String callbackListenerId) {
        ReplicationJobConfiguration replicationJobConfiguration = (ReplicationJobConfiguration) job.getJobConfiguration();
        Variant variant =
                contentManager.getVariant(replicationJobConfiguration.getVariant())
                        .orElseThrow(() -> new OmakaseRuntimeException("Unable to retrieve variant " + replicationJobConfiguration.getVariant()));
        String sourceRepositoryId = replicationJobConfiguration.getSourceRepositories().get(0);
        String destinationRepositoryId = replicationJobConfiguration.getDestinationRepositories().get(0);

        ImmutableList.Builder<Class<? extends PipelineStage>> stageBuilder = ImmutableList.builder();
        stageBuilder.add(ReplicationPrepareStage.class);

        if (repositoryManager.doesRepositoryRequireRestore(sourceRepositoryId)) {
            stageBuilder.add(RestoreStage.class);
        }

        if (repositoryManager.doesRepositoryRequireMultipartUpload(destinationRepositoryId) && !VariantType.HLS_MANIFEST.equals(variant.getType())) {
                stageBuilder.add(MultipartPrepareStage.class);
        }
        stageBuilder.add(TransferStage.class);

        return new Pipeline(job.getId(), WORKFLOW_OBJECT_NAME, callbackListenerId, stageBuilder.build(), TransferFailureStage.class);

    }


}
