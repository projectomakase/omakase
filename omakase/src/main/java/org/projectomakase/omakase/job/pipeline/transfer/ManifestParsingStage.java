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
import org.projectomakase.omakase.callback.Callbacks;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.job.configuration.ManifestType;
import org.projectomakase.omakase.job.pipeline.transfer.delegate.TransferDelegateResolver;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.pipeline.PipelineExecutor;
import org.projectomakase.omakase.pipeline.Pipelines;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.providers.manifest.Manifest;
import org.projectomakase.omakase.task.providers.manifest.ManifestFile;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskOutput;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * {@link PipelineStage} implementation that parses and ingests manifests.
 *
 * @author Richard Lucas
 */
public class ManifestParsingStage implements PipelineStage {

    private static final Logger LOGGER = Logger.getLogger(ManifestParsingStage.class);

    private static final String DEFAULT_MANIFEST_DESCRIPTION = "stream";

    @Inject
    TaskManager taskManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    TransferDelegateResolver transferDelegateResolver;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {
        String pipelineId = pipelineContext.getPipelineId();
        String jobId = pipelineContext.getObjectId();

        int priority = Integer.parseInt(Pipelines.getPipelineProperty(pipelineContext, "priority"));
        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "destinationRepositoryId");

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

        // safe to assume only one manifest as this is enforced by the job configuration validator
        ManifestTransferFile
                manifestTransferFile = TransferPipeline.getManifestTransferFromPipelineContext(pipelineContext).getManifestTransferFiles().stream().findFirst()
                .orElseThrow(() -> new OmakaseRuntimeException("no manifest transfer files are associated with the pipeline"));

        TaskGroup taskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, pipelineContext.getPipelineId(), PipelineExecutor.CALLBACK_LISTENER_ID));

        ManifestTransferTaskConfiguration configuration = createManifestTransferConfiguration(manifestTransferFile);
        String description = "Parse and ingest " + manifestTransferFile.getSource() + " to repository" + repositoryId;
        Task task = taskManager.createTask(taskGroup, new Task("MANIFEST_TRANSFER", description, priority, configuration));
        propertiesBuilder.put(task.getId(), manifestTransferFile.getId());

        return PipelineStageResult.builder(pipelineId, PipelineStageStatus.QUEUED).addProperties(propertiesBuilder.build()).build();
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        TaskStatus groupStatus = TaskStatus.valueOf(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupStatus"));

        PipelineStageResult result;

        switch (groupStatus) {
            case EXECUTING:
                result = PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.EXECUTING).build();
                break;
            case COMPLETED:
                result = handledCompleted(pipelineContext, callbackEvent);
                break;
            case FAILED_DIRTY:
            case FAILED_CLEAN:
                result = PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).build();
                break;
            default:
                String message = "Manifest parsing stage failed due to unexpected task group status " + groupStatus;
                LOGGER.error(message);
                result = PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).addMessages(ImmutableSet.of(message)).build();
                break;
        }

        return result;
    }

    private static ManifestTransferTaskConfiguration createManifestTransferConfiguration(ManifestTransferFile manifestTransferFile) {
        return new ManifestTransferTaskConfiguration(manifestTransferFile.getSource(), manifestTransferFile.getDestination(), ImmutableList.of(Hashes.MD5));
    }


    private PipelineStageResult handledCompleted(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();
        String variantId = Pipelines.getPipelineProperty(pipelineContext, "variant");
        String jobId = pipelineContext.getObjectId();
        int priority = Integer.parseInt(Pipelines.getPipelineProperty(pipelineContext, "priority"));

        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "destinationRepositoryId");
        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(() -> new OmakaseRuntimeException("Repository " + repositoryId + " does not exist"));
        URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);


        Set<Task> tasks = taskManager.getTasks(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupId"));
        List<ManifestTransferFile> manifestTransferFiles = tasks.stream().map(task -> updateManifestTransferFiles(pipelineContext, task)).collect(ImmutableListCollector.toImmutableList());

        manifestTransferFiles.stream().filter(manifest -> !manifest.getChildManifests().isEmpty() && !manifest.getManifestFiles().isEmpty()).findAny().ifPresent(manifest -> {
            throw new OmakaseRuntimeException(manifest.getSource() + " should not contain both manifests and file uris");
        });

        List<ManifestTransferFile> childManifestTransferFiles = getChildManifestTransferFiles(pipelineContext, variantId, repository, repositoryUri, manifestTransferFiles);
        propertiesBuilder.put(TransferPipeline.MANIFEST_TRANSFER, updateManifestTransfer(pipelineContext, manifestTransferFiles, childManifestTransferFiles).toJson());

        if (!childManifestTransferFiles.isEmpty()) {
            return createManifestTransferTasks(pipelineContext, propertiesBuilder, repositoryId, jobId, priority, childManifestTransferFiles);
        } else {
            return createTransfer(pipelineContext, propertiesBuilder, variantId, repository, manifestTransferFiles, repositoryUri);
        }

    }

    private ManifestTransferFile createManifestTransferFile(PipelineContext pipelineContext, ManifestTransferFile parent, Manifest manifest, String variantId, Repository repository,
                                                            URI repositoryUri) {
        RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, repository.getId());

        URI source = TransferPipeline.getAbsoluteSourceUri(parent, manifest.getUri());
        String originalFileName = source.toString().replaceFirst(".*/([^/?]+).*", "$1");
        URI destination = TransferPipeline.getDestinationUri(repository.getType(), repositoryUri, repositoryFile.getRelativePath(), originalFileName);

        ManifestTransfer manifestTransfer = TransferPipeline.getManifestTransferFromPipelineContext(pipelineContext);

        String originalFilepath = TransferPipeline.getOriginalFilepath(manifestTransfer, parent, manifest.getUri());

        return ManifestTransferFile
                .builder().repositoryFileId(repositoryFile.getId()).originalFilename(originalFileName).originalFilepath(originalFilepath).description(manifest.getDescription())
                .source(source).destination(destination).build();
    }

    private List<ManifestTransferFile> getChildManifestTransferFiles(PipelineContext pipelineContext, String variantId, Repository repository, URI repositoryUri,
                                                                     List<ManifestTransferFile> manifestTransferFiles) {
        return manifestTransferFiles.stream()
                .map(manifest -> manifest.getChildManifests().stream().map(uri -> createManifestTransferFile(pipelineContext, manifest, uri, variantId, repository, repositoryUri))
                        .collect(ImmutableListCollector.toImmutableList()))
                .collect(ImmutableListsCollector.toImmutableList());
    }

    private static ManifestTransferFile updateManifestTransferFiles(PipelineContext pipelineContext, Task task) {
        ManifestTransferTaskOutput taskOutput = (ManifestTransferTaskOutput) task.getOutput().orElseThrow(() -> new OmakaseRuntimeException("Task " + task.getId() + " is missing required output"));

        ManifestTransferFile manifestTransferFile = TransferPipeline.getManifestTransferFileFromPipelineContext(pipelineContext, task.getId());

        return ManifestTransferFile
                .builder(manifestTransferFile).size(taskOutput.getSize()).outputHashes(taskOutput.getHashes()).childManifests(taskOutput.getManifests())
                .manifestFiles(taskOutput.getFiles()).build();
    }

    private static ManifestTransfer updateManifestTransfer(PipelineContext pipelineContext, List<ManifestTransferFile> manifestTransferFiles, List<ManifestTransferFile> childManifestTransferFiles) {
        ManifestTransfer manifestTransfer = TransferPipeline.getManifestTransferFromPipelineContext(pipelineContext);

        List<String> manifestTransferFileIds = manifestTransferFiles.stream().map(ManifestTransferFile::getId).collect(ImmutableListCollector.toImmutableList());

        List<ManifestTransferFile> previous =
                manifestTransfer.getManifestTransferFiles().stream().filter(manifestTransferFile -> !manifestTransferFileIds.contains(manifestTransferFile.getId())).collect(
                        ImmutableListCollector.toImmutableList());

        ImmutableList.Builder<ManifestTransferFile> manifestTransferFileListBuilder = ImmutableList.builder();
        return new ManifestTransfer(manifestTransfer.getRootPath(), manifestTransfer.getManifestType(), manifestTransferFileListBuilder
                .addAll(previous).addAll(manifestTransferFiles)
                .addAll(childManifestTransferFiles)
                .build());
    }

    private PipelineStageResult createManifestTransferTasks(PipelineContext pipelineContext, ImmutableMap.Builder<String, String> propertiesBuilder, String repositoryId, String jobId, int priority,
                                                            List<ManifestTransferFile> childManifestTransferFiles) {
        TaskGroup taskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, pipelineContext.getPipelineId(), PipelineExecutor.CALLBACK_LISTENER_ID));
        childManifestTransferFiles.forEach(manifest -> {
            ManifestTransferTaskConfiguration configuration = createManifestTransferConfiguration(manifest);
            String description = "Parse and ingest " + manifest.getSource() + " to repository" + repositoryId;
            Task task = taskManager.createTask(taskGroup, new Task("MANIFEST_TRANSFER", description, priority, configuration));
            propertiesBuilder.put(task.getId(), manifest.getId());
        });
        return PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.EXECUTING).addProperties(propertiesBuilder.build()).build();
    }

    private PipelineStageResult createTransfer(PipelineContext pipelineContext, ImmutableMap.Builder<String, String> propertiesBuilder, String variantId, Repository repository,
                                               List<ManifestTransferFile> manifestTransferFiles, URI repositoryUri) {

        ManifestType manifestType = ManifestType.valueOf(Pipelines.getPipelineProperty(pipelineContext, "manifestType"));

        // The transfer will consist of either n transfer groups each with n transfer files if the manifest is HLS or n transfer groups with 1 transfer file per group in non HLS
        if (ManifestType.HLS.equals(manifestType)) {
            List<TransferFileGroup> transferFileGroups =
                    manifestTransferFiles.stream().map(manifest -> createTransferGroup(pipelineContext, manifest, variantId, repository, repositoryUri)).collect(ImmutableListCollector.toImmutableList());
            propertiesBuilder.put(TransferPipeline.TRANSFER, new Transfer(transferFileGroups).toJson());
        } else {
            List<TransferFileGroup> transferFileGroups = manifestTransferFiles.stream().map(manifest -> manifest.getManifestFiles().stream().map(uri -> TransferFileGroup.builder()
                    .transferFiles(ImmutableList.of(createTransferFile(pipelineContext, manifest, uri, variantId, repository, repositoryUri)))
                    .description(manifest.getDescription().orElse(DEFAULT_MANIFEST_DESCRIPTION))
                    .build()).collect(ImmutableListCollector.toImmutableList())).collect(ImmutableListsCollector.toImmutableList());
            propertiesBuilder.put(TransferPipeline.TRANSFER, new Transfer(transferFileGroups).toJson());

        }
        return PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.COMPLETED).addProperties(propertiesBuilder.build()).build();
    }

    private TransferFileGroup createTransferGroup(PipelineContext pipelineContext, ManifestTransferFile parent, String variantId, Repository repository, URI repositoryUri) {
        List<TransferFile> transferFiles = parent.getManifestFiles().stream().map(uri -> createTransferFile(pipelineContext, parent, uri, variantId, repository, repositoryUri)).collect(
                ImmutableListCollector.toImmutableList());

        return TransferFileGroup.builder().transferFiles(transferFiles).description(parent.getDescription().orElse(DEFAULT_MANIFEST_DESCRIPTION)).build();
    }

    private TransferFile createTransferFile(PipelineContext pipelineContext, ManifestTransferFile manifestTransferFile, ManifestFile manifestFile, String variantId, Repository repository, URI repositoryUri) {
        RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, repository.getId());
        URI source = TransferPipeline.getAbsoluteSourceUri(manifestTransferFile, manifestFile.getUri());
        String originalFileName = source.toString().replaceFirst(".*/([^/?]+).*", "$1");
        URI destination = TransferPipeline.getDestinationUri(repository.getType(), repositoryUri, repositoryFile.getRelativePath(), originalFileName);
        ManifestTransfer manifestTransfer = TransferPipeline.getManifestTransferFromPipelineContext(pipelineContext);
        String originalFilepath = TransferPipeline.getOriginalFilepath(manifestTransfer, manifestTransferFile, manifestFile.getUri());
        TransferFile.Builder transferFileBuilder =  TransferFile.builder()
                .destinationRepositoryFileId(repositoryFile.getId())
                .originalFilename(originalFileName)
                .originalFilepath(originalFilepath)
                .source(source)
                .destination(destination);
        manifestFile.getSize().ifPresent(transferFileBuilder::size);
        return transferFileBuilder.build();
    }
}
