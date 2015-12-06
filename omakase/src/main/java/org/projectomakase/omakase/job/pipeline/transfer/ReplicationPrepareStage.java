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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileSearchBuilder;
import org.projectomakase.omakase.content.VariantFileType;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.configuration.ReplicationJobConfiguration;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.search.SearchResult;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pipeline Stage used to prepare a replication.
 *
 * @author Richard Lucas
 */
public class ReplicationPrepareStage implements PipelineStage {

    @Inject
    JobManager jobManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    ContentManager contentManager;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {

        String pipelineId = pipelineContext.getPipelineId();

        String jobId = pipelineContext.getObjectId();
        Job job = jobManager.getJob(jobId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find job " + jobId + " for pipeline " + pipelineId));
        ReplicationJobConfiguration configuration = (ReplicationJobConfiguration) job.getJobConfiguration();
        String variantId = configuration.getVariant();

        String sourceRepositoryId = configuration.getSourceRepositories().get(0);
        String destinationRepositoryId = configuration.getDestinationRepositories().get(0);

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();
        propertiesBuilder.put("priority", Long.toString(job.getPriority()));
        propertiesBuilder.put("variant", variantId);
        propertiesBuilder.put("sourceRepositoryId", sourceRepositoryId);
        propertiesBuilder.put("destinationRepositoryId", destinationRepositoryId);
        propertiesBuilder.put("transferType", "REPLICATION");

        URI sourceRepositoryUri = repositoryManager.getRepositoryUri(sourceRepositoryId);
        Repository destinationRepository =
                repositoryManager.getRepository(destinationRepositoryId).orElseThrow(() -> new OmakaseRuntimeException("Repository " + destinationRepositoryId + " does not exist"));
        URI destinationRepositoryUri = repositoryManager.getRepositoryUri(destinationRepositoryId);
        ImmutableSet.Builder<Class<? extends PipelineStage>> additionalPipelineStages = ImmutableSet.builder();

        SearchResult<VariantFile> searchResult = contentManager.findVariantFiles(variantId, new VariantFileSearchBuilder().count(-1).build());

        List<TransferFileGroup> transferFileGroups = searchResult.getRecords().stream()
                .map(variantFile -> createTransferFileGroup(variantId, sourceRepositoryUri, sourceRepositoryId, destinationRepository, destinationRepositoryUri, variantFile))
                .collect(ImmutableListCollector.toImmutableList());
        Transfer transfer = new Transfer(transferFileGroups);
        propertiesBuilder.put(TransferPipeline.TRANSFER, transfer.toJson());

        getMultipartUploadInfo(destinationRepositoryId).ifPresent(multipartUploadInfo -> propertiesBuilder.put("multipartUploadInfo", multipartUploadInfo.toJson()));

        if (!TransferPipeline.doesTransferHaveFiles(transfer)) {
            return PipelineStageResult.builder(pipelineId, PipelineStageStatus.FAILED).addProperties(propertiesBuilder.build())
                    .addMessages(ImmutableSet.of("Failed to find any files to replicate")).build();
        } else {
            return PipelineStageResult.builder(pipelineId, PipelineStageStatus.COMPLETED).addProperties(propertiesBuilder.build()).addAdditionalPipelineStages(additionalPipelineStages.build())
                    .build();
        }
    }

    private Optional<MultipartUploadInfo> getMultipartUploadInfo(String repositoryId) {
        if (repositoryManager.doesRepositoryRequireMultipartUpload(repositoryId)) {
            return Optional.of(repositoryManager.getMultipartUploadInfoForRepository(repositoryId));
        } else {
            return Optional.empty();
        }
    }

    // manifest

    private TransferFileGroup createTransferFileGroup(String variantId, URI sourceRepositoryUri, String sourceRepositoryId, Repository destinationRepository, URI destinationRepositoryUri,
                                                      VariantFile variantFile) {
        if (VariantFileType.VIRTUAL.equals(variantFile.getType())) {
            Set<VariantFile> childVariantFiles = contentManager.getChildVariantFiles(variantId, variantFile.getId());

            List<TransferFile> transferFiles = childVariantFiles.stream()
                    .map(childVariantFile -> createTransferFileForChildVariantFile(variantId, sourceRepositoryUri,
                                                                                   repositoryManager.getRepositoryFileForVariantFile(sourceRepositoryId, childVariantFile.getId()).get(),
                                                                                   destinationRepository, destinationRepositoryUri, childVariantFile)).collect(ImmutableListCollector.toImmutableList());

            return TransferFileGroup.builder().description(variantFile.getVariantFilename()).transferFiles(transferFiles).build();
        } else {
            RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(sourceRepositoryId, variantFile.getId()).get();

            return TransferFileGroup.builder().transferFiles(ImmutableList.of(createTransferFile(variantId, sourceRepositoryUri, repositoryFile, destinationRepository, destinationRepositoryUri)))
                    .build();
        }
    }

    private TransferFile createTransferFile(String variantId, URI sourceRepositoryUri, RepositoryFile sourceRepositoryFile, Repository destinationRepository, URI destinationRepositoryUri) {
        URI source = Throwables.returnableInstance(() -> new URI(sourceRepositoryUri.toString() + "/" + sourceRepositoryFile.getRelativePath()));
        VariantFile variantFile = contentManager.getVariantFile(variantId, sourceRepositoryFile.getVariantFileId())
                .orElseThrow(() -> new NotFoundException("Unable to find variant file " + sourceRepositoryFile.getVariantFileId()));

        RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, destinationRepository.getId(), variantFile.getId());
        URI destination = TransferPipeline.getDestinationUri(destinationRepository.getType(), destinationRepositoryUri, repositoryFile.getRelativePath(), variantFile.getOriginalFilename());
        return TransferFile.builder().originalFilename(variantFile.getOriginalFilename()).source(source).destination(destination).size(variantFile.getSize())
                .sourceRepositoryFileId(sourceRepositoryFile.getId()).destinationRepositoryFileId(repositoryFile.getId()).build();
    }

    private TransferFile createTransferFileForChildVariantFile(String variantId, URI sourceRepositoryUri, RepositoryFile sourceRepositoryFile, Repository destinationRepository,
                                                               URI destinationRepositoryUri,
                                                               VariantFile childVariantFile) {
        URI source = Throwables.returnableInstance(() -> new URI(sourceRepositoryUri.toString() + "/" + sourceRepositoryFile.getRelativePath()));

        RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, destinationRepository.getId(), childVariantFile.getId());
        URI destination =
                TransferPipeline.getDestinationUri(destinationRepository.getType(), destinationRepositoryUri, repositoryFile.getRelativePath(), childVariantFile.getOriginalFilename());
        return TransferFile.builder().originalFilename(childVariantFile.getOriginalFilename()).source(source).destination(destination).size(childVariantFile.getSize())
                .sourceRepositoryFileId(sourceRepositoryFile.getId()).destinationRepositoryFileId(repositoryFile.getId()).build();
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        throw new UnsupportedOperationException();
    }
}
