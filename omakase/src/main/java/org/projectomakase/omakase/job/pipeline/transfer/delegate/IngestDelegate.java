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
package org.projectomakase.omakase.job.pipeline.transfer.delegate;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileHash;
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantType;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.job.pipeline.transfer.ManifestTransfer;
import org.projectomakase.omakase.job.pipeline.transfer.ManifestTransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.Transfer;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.pipeline.transfer.TransferPipeline;
import org.projectomakase.omakase.job.pipeline.transfer.client.TransferClient;
import org.projectomakase.omakase.job.pipeline.transfer.client.TransferClientResolver;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.pipeline.Pipelines;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.spi.TaskOutput;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Ingest {@link TransferDelegate} implementation.
 *
 * @author Richard Lucas
 */
public class IngestDelegate implements TransferDelegate {

    private static final Logger LOGGER = Logger.getLogger(IngestDelegate.class);

    @Inject
    RepositoryManager repositoryManager;
    @Inject
    ContentManager contentManager;
    @Inject
    TransferClientResolver transferClientResolver;

    @Override
    public TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup) {
        return getTransferClient(transferFileGroup).initiateTransferFileGroup(transferFileGroup);
    }

    @Override
    public TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput) {
        return transferClientResolver.resolve(transferFileGroup).completeTransferFileGroup(transferFileGroup, taskOutput);
    }

    @Override
    public void abortTransferFileGroup(TransferFileGroup transferFileGroup) {
        getTransferClient(transferFileGroup).abortTransferFileGroup(transferFileGroup);
    }

    @Override
    public Task createTask(PipelineContext pipelineContext, TransferFileGroup transferFileGroup, TaskGroup taskGroup) {
        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "destinationRepositoryId");
        int priority = Integer.parseInt(Pipelines.getPipelineProperty(pipelineContext, "priority"));

        String description;
        if (transferFileGroup.getTransferFiles().size() > 1) {
            description = "Ingest " + transferFileGroup.getDescription().get() + " to repository" + repositoryId;
        } else {
            description = "Ingest " + transferFileGroup.getTransferFiles().get(0).getSource() + " to repository" + repositoryId;
        }
        return getTransferClient(transferFileGroup).createTask(transferFileGroup, taskGroup, description, priority);

    }

    @Override
    public void updateContentRepository(PipelineContext pipelineContext, Transfer transfer) {
        String variantId = Pipelines.getPipelineProperty(pipelineContext, "variant");
        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "destinationRepositoryId");

        Optional.ofNullable(pipelineContext.getProperties().get(TransferPipeline.MANIFEST_TRANSFER)).ifPresent(json -> updateVariantType(variantId, repositoryId, json));

        transfer.getTransferFileGroups().forEach(transferFileGroup -> updateContentRepositoryWithTransferFileGroup(variantId, repositoryId, transferFileGroup));
        associateVariantWithRepository(repositoryId, variantId);
    }

    @Override
    public void cleanupContentRepository(PipelineContext pipelineContext) {
        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "destinationRepositoryId");

        Optional.ofNullable(pipelineContext.getProperties().get(TransferPipeline.MANIFEST_TRANSFER)).ifPresent(json -> {
            ManifestTransfer manifestTransfer = ManifestTransfer.fromJson(json);
            manifestTransfer.getManifestTransferFiles().forEach(manifestTransferFile -> deleteRepositoryFile(repositoryId, manifestTransferFile));
        });

        Transfer transfer = TransferPipeline.getTransferFromPipelineContext(pipelineContext);
        transfer.getTransferFileGroups().forEach(transferFileGroup -> transferFileGroup.getTransferFiles().forEach(transferFile -> deleteRepositoryFile(repositoryId, transferFile)));
    }

    private String getDestinationRepositoryFileId(TransferFile transferFile) {
        return transferFile.getDestinationRepositoryFileId().orElseThrow(() -> new OmakaseRuntimeException("Destination Repository File Id is not set"));
    }

    private void updateVariantType(String variantId, String repositoryId, String json) {
        ManifestTransfer manifestTransfer = ManifestTransfer.fromJson(json);
        manifestTransfer.getManifestTransferFiles().forEach(manifestTransferFile -> updateContentRepositoryFromManifestTransferFile(variantId, repositoryId, manifestTransferFile));

        Variant variant = contentManager.getVariant(variantId).orElseThrow(() -> new OmakaseRuntimeException("Unable to retrieve variant " + variantId));
        VariantType variantType;
        switch (manifestTransfer.getManifestType()) {
            case DASH:
                variantType = VariantType.DASH_MANIFEST;
                break;
            case SMOOTH:
                variantType = VariantType.SMOOTH_MANIFEST;
                break;
            case HLS:
                variantType = VariantType.HLS_MANIFEST;
                break;
            default:
                variantType = VariantType.FILE;
                break;
        }
        variant.setType(variantType);
        contentManager.updateVariant(variant);
    }

    private void updateContentRepositoryFromManifestTransferFile(String variantId, String repositoryId, ManifestTransferFile manifestTransferFile) {
        List<VariantFileHash> variantFileHashes = manifestTransferFile.getOutputHashes().stream()
                .map(hash -> new VariantFileHash(hash.getValue(), hash.getAlgorithm()))
                .collect(ImmutableListCollector.toImmutableList());

        VariantFile variantFile = new VariantFile(manifestTransferFile.getOriginalFilename(), manifestTransferFile.getSize().get(), manifestTransferFile.getOriginalFilename(),
                                                  manifestTransferFile.getOriginalFilepath(), variantFileHashes);
        variantFile = contentManager.createVariantFile(variantId, variantFile);
        updateRepositoryFile(repositoryId, variantFile, manifestTransferFile.getRepositoryFileId());
    }

    private void updateContentRepositoryWithTransferFileGroup(String variantId, String repositoryId, TransferFileGroup transferFileGroup) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            VariantFile virtualVariantFile = contentManager
                    .createVariantFile(variantId, new VariantFile(transferFileGroup.getDescription().orElseThrow(() -> new OmakaseRuntimeException("Missing expected transfer group description"))));
            transferFileGroup.getTransferFiles().forEach(transferFile -> updateContentRepositoryWithTransferFile(virtualVariantFile, transferFile, repositoryId, variantId));
        } else {
            TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
            String originalFileName = transferFile.getOriginalFilename();
            List<VariantFileHash> variantFileHashes =
                    transferFile.getOutputHashes().stream().map(hash -> new VariantFileHash(hash.getValue(), hash.getAlgorithm())).collect(ImmutableListCollector.toImmutableList());
            VariantFile variantFile =
                    contentManager.createVariantFile(variantId, new VariantFile(originalFileName, transferFile.getSize().get(), originalFileName, originalFileName, variantFileHashes));
            String repositoryFileId = transferFile.getDestinationRepositoryFileId().orElseThrow(() -> new OmakaseRuntimeException("Destination Repository File Id is not set"));
            RepositoryFile repositoryFile =
                    repositoryManager.getRepositoryFile(repositoryId, repositoryFileId).orElseThrow(() -> new NotFoundException("Repository File" + repositoryFileId + " does not exist"));
            repositoryFile.setVariantFileId(variantFile.getId());
            transferFile.getArchiveId().ifPresent(repositoryFile::setRelativePath);
            repositoryManager.updateRepositoryFile(repositoryFile);
        }
    }

    private void updateContentRepositoryWithTransferFile(VariantFile virtualVariantFile, TransferFile transferFile, String repositoryId, String variantId) {
        List<VariantFileHash> variantFileHashes = transferFile.getOutputHashes().stream().map(hash -> new VariantFileHash(hash.getValue(), hash.getAlgorithm())).collect(
                ImmutableListCollector.toImmutableList());
        VariantFile child = new VariantFile(transferFile.getOriginalFilename(), transferFile.getSize().orElseThrow(() -> new OmakaseRuntimeException("Missing expected transfer file size")),
                                            transferFile.getOriginalFilename(), transferFile.getOriginalFilepath(), variantFileHashes);
        VariantFile variantFile = contentManager.createChildVariantFile(variantId, virtualVariantFile, child);
        updateRepositoryFile(repositoryId, variantFile, getDestinationRepositoryFileId(transferFile));
    }

    private void updateRepositoryFile(String repositoryId, VariantFile variantFile, String repositoryFileId) {
        RepositoryFile repositoryFile =
                repositoryManager.getRepositoryFile(repositoryId, repositoryFileId).orElseThrow(() -> new NotFoundException("Repository File" + repositoryFileId + " does not exist"));
        repositoryFile.setVariantFileId(variantFile.getId());
        repositoryManager.updateRepositoryFile(repositoryFile);
    }

    private void associateVariantWithRepository(String repositoryId, String variantId) {
        Repository repository = repositoryManager.getRepository(repositoryId).get();
        VariantRepository variantRepository = new VariantRepository(repository.getId(), repository.getRepositoryName(), repository.getType());
        contentManager.associateVariantToRepositories(variantId, ImmutableList.of(variantRepository));
    }

    private void deleteRepositoryFile(String repositoryId, ManifestTransferFile manifestTransferFile) {
        try {
            repositoryManager.deleteRepositoryFile(repositoryId, manifestTransferFile.getRepositoryFileId());
        } catch (Exception e) {
            LOGGER.error("Failed to delete repository file. Reason: " + e.getMessage(), e);
        }
    }

    private void deleteRepositoryFile(String repositoryId, TransferFile transferFile) {
        try {
            transferFile.getDestinationRepositoryFileId().ifPresent(repoFileId -> repositoryManager.deleteRepositoryFile(repositoryId, repoFileId));
        } catch (Exception e) {
            LOGGER.error("Failed to delete repository file. Reason: " + e.getMessage(), e);
        }
    }

    private TransferClient getTransferClient(TransferFileGroup transferFileGroup) {
        return transferClientResolver.resolve(transferFileGroup);
    }
}
