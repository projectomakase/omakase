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
import com.google.common.primitives.Ints;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileSearchBuilder;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.ExportJobConfiguration;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.restore.RestoreTaskOutput;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class ExportDelegate {

    @Inject
    JobManager jobManager;
    @Inject
    TaskManager taskManager;
    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    JobScenarioTools jobScenarioTools;


    public void execute(JobScenario.ExportJobInfo exportJobInfo, Worker worker, Variant variant) {
        Job exportJob = jobManager.createJob(Job.Builder.build(j -> {
            j.setJobType(JobType.EXPORT);
            j.setStatus(JobStatus.QUEUED);
            j.setJobConfiguration(getExportJobConfig(exportJobInfo, variant));
        }));

        jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(exportJob.getId(), TaskStatus.QUEUED);

        if (restore(exportJobInfo, exportJob)) {
            transfer(exportJobInfo, worker, variant, exportJob);
        }
    }

    private ExportJobConfiguration getExportJobConfig(JobScenario.ExportJobInfo exportJobInfo, Variant variant) {
        return ExportJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setRepositories(ImmutableList.of(getRepositoryId(exportJobInfo)));
            config.setLocations(ImmutableList.of("file:/test"));
        });
    }

    private boolean restore(JobScenario.ExportJobInfo exportJobInfo, Job exportJob) {
        if (repositoryManager.doesRepositoryRequireRestore(getRepositoryId(exportJobInfo))) {
            TaskGroup taskGroup = taskManager.getTaskGroups(exportJob.getId()).stream().findFirst().get();
            if (JobScenario.JobFailureType.RESTORE.equals(exportJobInfo.getJobFailureType())) {
                taskManager.getTasks(taskGroup.getId()).forEach(task -> taskManager.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed", 0)));
                jobScenarioTools.waitUntilJobHasStatus(exportJob.getId(), JobStatus.FAILED);
                return false;
            } else {
                taskManager.getTasks(taskGroup.getId()).forEach(task -> taskManager
                        .updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.COMPLETED, "Succeeded", 100, new RestoreTaskOutput(Throwables.returnableInstance(() -> new URI("test-archive-id"))))));
                jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(taskGroup.getId(), TaskStatus.COMPLETED);
                return true;
            }
        }
        return true;
    }

    private void transfer(JobScenario.ExportJobInfo exportJobInfo, Worker worker, Variant variant, Job exportJob) {

        int numberOfFiles = Ints.checkedCast(contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getTotalRecords());
        if (JobScenario.JobFailureType.TRANSFER.equals(exportJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, numberOfFiles, "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(exportJob.getId(), JobStatus.FAILED);
        } else if (JobScenario.JobFailureType.NONE.equals(exportJobInfo.getJobFailureType())) {
            String repositoryId = getRepositoryId(exportJobInfo);
            URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);
            List<VariantFile> variantFiles = contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords();
            variantFiles.forEach(variantFile -> {
                RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(repositoryId, variantFile.getId()).get();
                String contentInfoUri = repositoryUri + "/" + repositoryFile.getRelativePath();
                jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, new TransferTaskOutput(
                        ImmutableList.of(new ContentInfo(getUriFromString(contentInfoUri), 1024, Collections.singletonList(new Hash("MD5", "abc-def")))))));
            });
            jobScenarioTools.waitUntilJobHasStatus(exportJob.getId(), JobStatus.COMPLETED);
        }

        // validate job
        exportJob = jobManager.getJob(exportJob.getId()).get();
        if (JobScenario.JobFailureType.NONE.equals(exportJobInfo.getJobFailureType())) {
            assertThat(exportJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
        } else {
            assertThat(exportJob.getStatus()).isEqualTo(JobStatus.FAILED);
        }
    }

    private String getRepositoryId(JobScenario.ExportJobInfo exportJobInfo) {
        return jobScenarioTools.getRepositoryByName(exportJobInfo.getRepositoryName()).getId();
    }

    private URI getUriFromString(String uri) {
        return Throwables.returnableInstance(() -> new URI(uri));
    }
}
