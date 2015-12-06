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
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.job.pipeline.transfer.delegate.TransferDelegate;
import org.projectomakase.omakase.job.pipeline.transfer.delegate.TransferDelegateResolver;
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
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;

/**
 * {@link PipelineStage} implementation that transfers a list of {@link TransferFile} instances.
 * <p>
 * This stage is used when the transfer associated with pipeline only has a single transfer group. This stage performs multi-part uploads to repositories that require/support multi-part uploads.
 * </p>
 *
 * @author Richard Lucas
 */
public class TransferStage implements PipelineStage {

    private static final Logger LOGGER = Logger.getLogger(TransferStage.class);

    @Inject
    TaskManager taskManager;
    @Inject
    TransferDelegateResolver transferDelegateResolver;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {
        String pipelineId = pipelineContext.getPipelineId();
        String jobId = pipelineContext.getObjectId();
        // Using a two stage process in order to first initiate each transfer (which may fail)
        // and then only create tasks if all transfers initiate successfully.
        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();
        Transfer transfer = new Transfer(initiateTransferFileGroups(pipelineContext));
        TaskGroup taskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, pipelineContext.getPipelineId(), PipelineExecutor.CALLBACK_LISTENER_ID));
        transfer.getTransferFileGroups().forEach(transferFileGroup -> createTask(pipelineContext, taskGroup, propertiesBuilder, transferFileGroup));
        propertiesBuilder.put(TransferPipeline.TRANSFER, transfer.toJson());
        return PipelineStageResult.builder(pipelineId, PipelineStageStatus.QUEUED).addProperties(propertiesBuilder.build()).build();
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        TaskStatus groupStatus = TaskStatus.valueOf(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupStatus"));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Task Group status: " + groupStatus);
        }

        PipelineStageResult result;

        switch (groupStatus) {
            case EXECUTING:
                result = PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.EXECUTING).build();
                break;
            case COMPLETED:
                result = handleCompleted(pipelineContext, callbackEvent);
                break;
            case FAILED_DIRTY:
            case FAILED_CLEAN:
                result = PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).build();
                handleFailed(pipelineContext);
                break;
            default:
                String message = "Transfer stage failed due to unexpected task group status " + groupStatus;
                LOGGER.error(message);
                result = PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).addMessages(ImmutableSet.of(message)).build();
                break;
        }

        return result;
    }

    private ImmutableList<TransferFileGroup> initiateTransferFileGroups(PipelineContext pipelineContext) {
        return TransferPipeline.getTransferFromPipelineContext(pipelineContext)
                .getTransferFileGroups()
                .stream()
                .map(transferFileGroup -> getTransferDelegate(pipelineContext).initiateTransferFileGroup(transferFileGroup))
                .collect(toImmutableList());
    }

    private void createTask(PipelineContext pipelineContext, TaskGroup taskGroup, ImmutableMap.Builder<String, String> propertiesBuilder, TransferFileGroup transferFileGroup) {
        Task task = getTransferDelegate(pipelineContext).createTask(pipelineContext, transferFileGroup, taskGroup);
        propertiesBuilder.put(task.getId(), transferFileGroup.getId());
    }

    private PipelineStageResult handleCompleted(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        Set<Task> tasks = taskManager.getTasks(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupId"));

        // Using a two stage process in order to first complete each transfer (which may fail)
        // and then only update the content repository if all transfers complete successfully.
        List<TransferFileGroup> transferFileGroups;
        try {
            transferFileGroups = tasks.stream().map(task -> completeTransferFileGroup(pipelineContext, task)).collect(toImmutableList());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            handleFailed(pipelineContext);
            return PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).build();
        }
        Transfer transfer = new Transfer(transferFileGroups);
        getTransferDelegate(pipelineContext).updateContentRepository(pipelineContext, transfer);
        return PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.COMPLETED).build();
    }

    private TransferFileGroup completeTransferFileGroup(PipelineContext pipelineContext, Task task) {
        TransferFileGroup transferFileGroup = TransferPipeline.getTransferFileGroupFromPipelineContext(pipelineContext, task.getId());
        return getTransferDelegate(pipelineContext)
                .completeTransferFileGroup(transferFileGroup, task.getOutput().orElseThrow(() -> new OmakaseRuntimeException("Task " + task.getId() + " is missing required output")));
    }


    private void handleFailed(PipelineContext pipelineContext) {
        TransferPipeline.getTransferFromPipelineContext(pipelineContext)
                .getTransferFileGroups()
                .forEach(transferFileGroup -> abortTransfer(pipelineContext, transferFileGroup));
    }

    private void abortTransfer(PipelineContext pipelineContext, TransferFileGroup transferFileGroup) {
        try {
            getTransferDelegate(pipelineContext).abortTransferFileGroup(transferFileGroup);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private TransferDelegate getTransferDelegate(PipelineContext pipelineContext) {
        return transferDelegateResolver.resolve(Pipelines.getPipelineProperty(pipelineContext, "transferType"));
    }
}
