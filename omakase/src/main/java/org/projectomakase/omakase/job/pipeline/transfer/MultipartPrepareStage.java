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
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.callback.Callbacks;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.pipeline.PipelineExecutor;
import org.projectomakase.omakase.pipeline.Pipelines;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.providers.hash.HashInput;
import org.projectomakase.omakase.task.providers.hash.HashTaskConfiguration;
import org.projectomakase.omakase.task.providers.hash.HashTaskOutput;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * {@link PipelineStage} implementation that prepares transfers to be handled in multiple parts.
 * <p>
 * Breaks the transfer into multiple parts and generates the required hash values for each part.
 * </p>
 *
 * @author Richard Lucas
 */
public class MultipartPrepareStage implements PipelineStage {

    private static final Logger LOGGER = Logger.getLogger(MultipartPrepareStage.class);

    @Inject
    TaskManager taskManager;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {
        String pipelineId = pipelineContext.getPipelineId();
        String jobId = pipelineContext.getObjectId();

        int priority = Integer.parseInt(Pipelines.getPipelineProperty(pipelineContext, "priority"));

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

        MultipartUploadInfo multipartUploadInfo = getMultipartUploadInfo(pipelineContext);

        TaskGroup taskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, pipelineContext.getPipelineId(), PipelineExecutor.CALLBACK_LISTENER_ID));

        Transfer transfer = new Transfer(TransferPipeline.getTransferFromPipelineContext(pipelineContext)
                                                 .getTransferFileGroups()
                                                 .stream()
                                                 .map(transferFileGroup -> updateTransferFileGroup(multipartUploadInfo, transferFileGroup, taskGroup, priority, propertiesBuilder))
                                                 .collect(ImmutableListCollector.toImmutableList()));

        propertiesBuilder.put(TransferPipeline.TRANSFER, transfer.toJson());
        return PipelineStageResult.builder(pipelineId, PipelineStageStatus.QUEUED).addProperties(propertiesBuilder.build()).build();
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        TaskStatus groupStatus = TaskStatus.valueOf(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupStatus"));

        PipelineStageStatus pipelineStageStatus;

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

        switch (groupStatus) {
            case EXECUTING:
                pipelineStageStatus = PipelineStageStatus.EXECUTING;
                break;
            case COMPLETED:
                pipelineStageStatus = PipelineStageStatus.COMPLETED;
                handledCompleted(pipelineContext, callbackEvent, propertiesBuilder);
                break;
            case FAILED_DIRTY:
            case FAILED_CLEAN:
                pipelineStageStatus = PipelineStageStatus.FAILED;
                break;
            default:
                LOGGER.error("Invalid task status " + groupStatus);
                pipelineStageStatus = PipelineStageStatus.FAILED;
                break;
        }

        return PipelineStageResult.builder(pipelineContext.getPipelineId(), pipelineStageStatus).addProperties(propertiesBuilder.build()).build();
    }

    private TransferFileGroup updateTransferFileGroup(MultipartUploadInfo multipartUploadInfo, TransferFileGroup transferFileGroup, TaskGroup taskGroup, int priority,
                                                      ImmutableMap.Builder<String, String> propertiesBuilder) {
        List<TransferFile> transferFiles = transferFileGroup.getTransferFiles()
                .stream()
                .map(transferFile -> TransferFile.builder(transferFile).partSize(multipartUploadInfo.getPartSize()).build())
                .collect(ImmutableListCollector.toImmutableList());
        // multipart prepare should only ever be called on transfer groups with one file.
        TransferFile transferFile = transferFiles.get(0);
        HashTaskConfiguration configuration = createHashTaskConfiguration(transferFile, multipartUploadInfo);
        Task task = taskManager.createTask(taskGroup, new Task("HASH", "Generate hashes for " + configuration.getSource(), priority, configuration));
        propertiesBuilder.put(task.getId(), transferFileGroup.getId());
        return TransferFileGroup.builder(transferFileGroup).transferFiles(transferFiles).build();
    }

    private static HashTaskConfiguration createHashTaskConfiguration(TransferFile transferFile, MultipartUploadInfo multipartUploadInfo) {
        List<HashInput> hashInputs = Hashes.createByteRanges(transferFile.getPartSize().get(), transferFile.getSize().get()).stream()
                .map(byteRange -> multipartUploadInfo.getRequiredHashAlgorithms().stream().map(algorithm -> new HashInput(algorithm, byteRange.getFrom(), byteRange.getTo() + 1))
                        .collect(ImmutableListCollector.toImmutableList())).collect(ImmutableListsCollector.toImmutableList());
        return new HashTaskConfiguration(transferFile.getSource(), hashInputs);
    }

    private void handledCompleted(PipelineContext pipelineContext, CallbackEvent callbackEvent, ImmutableMap.Builder<String, String> propertiesBuilder) {
        Set<Task> tasks = taskManager.getTasks(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupId"));
        MultipartUploadInfo multipartUploadInfo = getMultipartUploadInfo(pipelineContext);
        List<TransferFileGroup> transferFileGroups = tasks.stream().map(task -> updateTransfers(pipelineContext, task, multipartUploadInfo)).collect(ImmutableListCollector.toImmutableList());
        propertiesBuilder.put(TransferPipeline.TRANSFER, new Transfer(transferFileGroups).toJson());
    }

    private static MultipartUploadInfo getMultipartUploadInfo(PipelineContext pipelineContext) {
        return MultipartUploadInfo.fromJson(Pipelines.getPipelineProperty(pipelineContext, "multipartUploadInfo"));
    }

    private static TransferFileGroup updateTransfers(PipelineContext pipelineContext, Task task, MultipartUploadInfo multipartUploadInfo) {
        final TransferFileGroup transferFileGroup = TransferPipeline.getTransferFileGroupFromPipelineContext(pipelineContext, task.getId());
        // multipart prepare should only ever be called on transfer groups with one file.
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        final HashTaskOutput hashTaskOutput = (HashTaskOutput) task.getOutput().orElseThrow(() -> new OmakaseRuntimeException("Task " + task.getId() + " is missing required output"));
        List<AWSUploadPart> parts = mapTaskOutputToUploadParts(hashTaskOutput, multipartUploadInfo);
        return TransferFileGroup.builder(transferFileGroup).transferFiles(ImmutableList.of(TransferFile.builder(transferFile).parts(parts).build())).build();
    }

    private static List<AWSUploadPart> mapTaskOutputToUploadParts(HashTaskOutput hashTaskOutput, MultipartUploadInfo multipartUploadInfo) {
        Comparator<Hash> hashComparator = (o1, o2) -> o1.getOffset().compareTo(o2.getOffset());

        // This is fragile as it assumes the repository provider implementation returns the hash algorithms in this expected order
        String signingHashAlgorithm = multipartUploadInfo.getRequiredHashAlgorithms().get(0);
        String partHashAlgorithm = multipartUploadInfo.getRequiredHashAlgorithms().get(1);

        List<Hash> signingHashes = hashTaskOutput.getHashes().stream().filter(hash -> hash.getAlgorithm().equals(signingHashAlgorithm)).sorted(hashComparator).collect(
                ImmutableListCollector.toImmutableList());
        List<Hash> partHashes = hashTaskOutput.getHashes().stream().filter(hash -> hash.getAlgorithm().equals(partHashAlgorithm)).sorted(hashComparator).collect(ImmutableListCollector.toImmutableList());

        return IntStream.range(0, signingHashes.size()).mapToObj(index -> {
            Hash signingHash = signingHashes.get(index);
            Hash partHash = partHashes.get(index);
            return new AWSUploadPart(index, signingHash.getOffset(), signingHash.getLength().get(), signingHash.getValue(), partHash.getValue());
        }).collect(ImmutableListCollector.toImmutableList());
    }
}
