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
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.callback.Callbacks;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.streams.Streams;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.configuration.DeleteVariantJobConfiguration;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.pipeline.PipelineExecutor;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.providers.delete.DeleteTaskConfiguration;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;

/**
 * Pipeline stage used to delete files from a repositories using the job and task framework.
 * <p>
 * The file may be deleted immediately by this stage if the repository supports synchronous remote deletion, otherwise a task will be created to delete the files asynchronously.
 * </p>
 *
 * @author Richard Lucas
 */
public class DeleteStage implements PipelineStage {

    private static final Logger LOGGER = Logger.getLogger(DeleteStage.class);

    @Inject
    JobManager jobManager;
    @Inject
    TaskManager taskManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    ContentManager contentManager;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {
        String pipelineId = pipelineContext.getPipelineId();
        String jobId = pipelineContext.getObjectId();
        Job job = jobManager.getJob(jobId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find job " + jobId + " for pipeline " + pipelineId));
        long priority = job.getPriority();
        DeleteVariantJobConfiguration configuration = (DeleteVariantJobConfiguration) job.getJobConfiguration();
        String variantId = configuration.getVariant();

        List<Task> tasks = configuration.getRepositories().stream().map(repositoryId -> deleteVariantFromRepositoryOrCreateTask(priority, configuration, variantId, repositoryId))
                .flatMap(Streams::optionalToStream).collect(ImmutableListCollector.toImmutableList());

        if (tasks.isEmpty()) {
            return PipelineStageResult.builder(pipelineId, PipelineStageStatus.COMPLETED).addMessages(ImmutableSet.of("Deleted variant " + variantId + " from repositories")).build();
        } else {
            TaskGroup taskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, pipelineContext.getPipelineId(), PipelineExecutor.CALLBACK_LISTENER_ID));
            tasks.forEach(task -> taskManager.createTask(taskGroup, task));
            return PipelineStageResult.builder(pipelineId, PipelineStageStatus.QUEUED).addMessages(ImmutableSet.of("Queued tasks to delete variant " + variantId + " from repositories")).build();
        }
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        TaskStatus groupStatus = TaskStatus.valueOf(Callbacks.getCallbackEventProperty(callbackEvent, "taskGroupStatus"));

        PipelineStageStatus pipelineStageStatus;

        switch (groupStatus) {
            case EXECUTING:
                pipelineStageStatus = PipelineStageStatus.EXECUTING;
                break;
            case COMPLETED:
                pipelineStageStatus = handleCompletedCallback(pipelineContext);
                break;
            case FAILED_DIRTY:
            case FAILED_CLEAN:
                pipelineStageStatus = handleFailedCallback(pipelineContext);
                break;
            default:
                LOGGER.error("Invalid task status " + groupStatus);
                pipelineStageStatus = PipelineStageStatus.FAILED;
                break;
        }

        return PipelineStageResult.builder(pipelineContext.getPipelineId(), pipelineStageStatus).build();
    }

    private PipelineStageStatus handleCompletedCallback(PipelineContext pipelineContext) {
        PipelineStageStatus pipelineStageStatus;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("All tasks completed for pipeline " + pipelineContext.getPipelineId());
        }

        pipelineStageStatus = PipelineStageStatus.COMPLETED;
        String jobId = pipelineContext.getObjectId();
        Job job = jobManager.getJob(jobId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find job " + jobId + " for pipeline " + pipelineContext.getPipelineId()));
        DeleteVariantJobConfiguration configuration = (DeleteVariantJobConfiguration) job.getJobConfiguration();
        String variantId = configuration.getVariant();
        configuration.getRepositories().forEach(repositoryId -> deleteVariantFromRemainingRepositories(variantId, repositoryId));
        return pipelineStageStatus;
    }


    private Optional<Task> deleteVariantFromRepositoryOrCreateTask(long priority, DeleteVariantJobConfiguration configuration, String variantId, String repositoryId) {
        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find repository " + repositoryId));
        if (repositoryManager.doesRepositorySupportRemoteDelete(repository)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Repository " + repositoryId + " supports remote delete. Deleting variant " + variantId + " from repository.");
            }
            repositoryManager.deleteVariantFromRepository(repositoryId, variantId);
            return Optional.empty();
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Repository " + repositoryId + " does not supports remote delete. Creating task to delete variant " + variantId + " from repository.");
            }
            return Optional.of(createTaskForAllRepositoryFiles(priority, configuration, variantId, repositoryId));
        }
    }

    private Task createTaskForAllRepositoryFiles(long priority, DeleteVariantJobConfiguration configuration, String variantId, String repositoryId) {
        // create a single task for all files
        URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);
        ImmutableList<RepositoryFile> repositoryFiles = repositoryManager.getRepositoryFilesForVariant(repositoryId, configuration.getVariant());
        System.out.println(repositoryFiles);
        List<URI> locations = repositoryFiles.stream().map(repositoryFile -> getSource(repositoryUri, repositoryFile)).collect(toImmutableList());
        return newTask(priority, variantId, repositoryId, locations);
    }

    private void deleteVariantFromRemainingRepositories(String variantId, String repositoryId) {
        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find repository " + repositoryId));
        // we only delete from repositories that don't support remote delete as remote delete repositories were handled in prepare
        if (!repositoryManager.doesRepositorySupportRemoteDelete(repository)) {
            repositoryManager.deleteVariantFromRepository(repositoryId, variantId);
        }
    }

    private static Task newTask(long priority, String variantId, String repositoryId, List<URI> locations) {
        return new Task("DELETE", "Delete variant " + variantId + " from repository " + repositoryId, priority, new DeleteTaskConfiguration(locations));
    }

    private static URI getSource(URI repositoryUri, RepositoryFile repositoryFile) {
        return Throwables.returnableInstance(() -> new URI(repositoryUri.toString() + "/" + repositoryFile.getRelativePath()));
    }

    private static PipelineStageStatus handleFailedCallback(PipelineContext pipelineContext) {
        PipelineStageStatus pipelineStageStatus;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Pipeline " + pipelineContext.getPipelineId() + " has failed tasks");
        }

        pipelineStageStatus = PipelineStageStatus.FAILED;
        return pipelineStageStatus;
    }
}
