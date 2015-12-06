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
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.exceptions.NotFoundException;
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
import org.projectomakase.omakase.task.providers.restore.RestoreTaskConfiguration;
import org.projectomakase.omakase.task.providers.restore.RestoreTaskOutput;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Pipeline stage used to restore repository files to online in their repository prior to use.
 * <p>
 * Requests a restore for each repository file that requires it and creates a task to monitor it's restore progress.
 * </p>
 *
 * @author Richard Lucas
 */
public class RestoreStage implements PipelineStage {

    private static final Logger LOGGER = Logger.getLogger(RestoreStage.class);

    @Inject
    TaskManager taskManager;
    @Inject
    RepositoryManager repositoryManager;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {

        String pipelineId = pipelineContext.getPipelineId();
        String jobId = pipelineContext.getObjectId();
        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "sourceRepositoryId");

        TaskGroup taskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, pipelineContext.getPipelineId(), PipelineExecutor.CALLBACK_LISTENER_ID));

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

        Transfer transfer = TransferPipeline.getTransferFromPipelineContext(pipelineContext);
        transfer.getTransferFileGroups().forEach(transferFileGroup -> {
            // restore should only ever be called on transfer groups with one file.
            TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
            RepositoryFile repositoryFile = repositoryManager.getRepositoryFile(repositoryId, transferFile.getSourceRepositoryFileId())
                    .orElseThrow(() -> new NotFoundException("Repository file " + transferFile.getSourceRepositoryFileId() + " does not exist"));
            String description = "Restore variant file " + repositoryFile.getVariantFileId() + " in repository " + repositoryId;

            //we currently always assume a restore is required and the task will be updated by a callback in the future.
            Task task = taskManager.createTask(taskGroup, new Task("RESTORE", description, 1, new RestoreTaskConfiguration(transferFile.getSource())), false);
            repositoryManager.restore(repositoryId, repositoryFile, task.getId());
            propertiesBuilder.put(task.getId(), transferFileGroup.getId());
        });

        return PipelineStageResult.builder(pipelineId, PipelineStageStatus.QUEUED).addProperties(propertiesBuilder.build()).build();
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {

        TaskStatus groupStatus = TaskStatus.valueOf(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupStatus"));
        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

        PipelineStageStatus pipelineStageStatus;

        // Nothing ever moves the restore tasks to executing, which means the group reports queued until all tasks have completed, failed (not ideal), this works around the problem for now.
        // The correct approach is to move the tasks to executing but this causes all sorts of issues with transaction boundaries.

        switch (groupStatus) {
            case QUEUED:
            case EXECUTING:
                pipelineStageStatus = PipelineStageStatus.EXECUTING;
                break;
            case COMPLETED:
                pipelineStageStatus = PipelineStageStatus.COMPLETED;
                Set<Task> tasks = taskManager.getTasks(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupId"));
                List<TransferFileGroup> transferFileGroups = tasks.stream().map(task -> createNewTransferFileGroup(task, pipelineContext)).collect(ImmutableListCollector.toImmutableList());
                propertiesBuilder.put(TransferPipeline.TRANSFER, new Transfer(transferFileGroups).toJson());
                break;
            case FAILED_DIRTY:
            case FAILED_CLEAN:
                pipelineStageStatus = PipelineStageStatus.FAILED;
                break;
            default:
                LOGGER.error("Invalid task group status " + groupStatus);
                pipelineStageStatus = PipelineStageStatus.FAILED;
                break;
        }

        return PipelineStageResult.builder(pipelineContext.getPipelineId(), pipelineStageStatus).addProperties(propertiesBuilder.build()).build();
    }

    private TransferFileGroup createNewTransferFileGroup(Task task, PipelineContext pipelineContext) {
        TransferFileGroup transferFileGroup = TransferPipeline.getTransferFileGroupFromPipelineContext(pipelineContext, task.getId());
        // restore should only ever be called on transfer groups with one file.
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        RestoreTaskOutput output = (RestoreTaskOutput) task.getOutput().orElseThrow(() -> new OmakaseRuntimeException("Task " + task.getId() + " is missing required output"));
        String repositoryId = Pipelines.getPipelineProperty(pipelineContext, "sourceRepositoryId");
        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(() -> new NotFoundException("repository " + repositoryId + " does not exist"));
        URI source = output.getDestination();

        if ("GLACIER".equals(repository.getType())) {
            URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);
            source = Throwables.returnableInstance(() -> new URI(repositoryUri + String.format("/jobs/%s/output", output.getDestination())));
        }

        return TransferFileGroup.builder(transferFileGroup).transferFiles(ImmutableList.of(TransferFile.builder(transferFile).source(source).build())).build();
    }
}
