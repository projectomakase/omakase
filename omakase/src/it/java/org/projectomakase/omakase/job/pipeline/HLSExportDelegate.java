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
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
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
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class HLSExportDelegate {

    @Inject
    JobManager jobManager;
    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    JobScenarioTools jobScenarioTools;


    public void execute(JobScenario.HLSExportJobInfo exportJobInfo, Worker worker, Variant variant) {
        Job exportJob = jobManager.createJob(Job.Builder.build(j -> {
            j.setJobType(JobType.EXPORT);
            j.setStatus(JobStatus.QUEUED);
            j.setJobConfiguration(getExportJobConfig(exportJobInfo, variant));
        }));

        jobScenarioTools.waitUntilJobHasStatus(exportJob.getId(), JobStatus.QUEUED);

        transfer(exportJobInfo, worker, variant, exportJob);
    }

    private ExportJobConfiguration getExportJobConfig(JobScenario.HLSExportJobInfo exportJobInfo, Variant variant) {
        return ExportJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setRepositories(ImmutableList.of(getRepositoryId(exportJobInfo)));
            config.setLocations(ImmutableList.of("file:/test"));
        });
    }


    private void transfer(JobScenario.HLSExportJobInfo exportJobInfo, Worker worker, Variant variant, Job exportJob) {
        String repositoryId = getRepositoryId(exportJobInfo);
        URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);

        List<VariantFile> variantFiles = contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords();
        variantFiles.forEach(variantFile -> {
            if (variantFile.getOriginalFilename() != null) {
                RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(repositoryId, variantFile.getId()).get();
                String contentInfoUri = repositoryUri + "/" + repositoryFile.getRelativePath();
                jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100,
                                                                                               new TransferTaskOutput(ImmutableList.of(new ContentInfo(getUriFromString(contentInfoUri), 1024, Collections.singletonList(new Hash("MD5", "abc-def")))))));
            } else {
                Set<VariantFile> childVariantFiles = contentManager.getChildVariantFiles(variant.getId(), variantFile.getId());
                List<String> fileUris = childVariantFiles.stream().map(childVariantFile -> {
                    RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(repositoryId, childVariantFile.getId()).get();
                    return repositoryUri + "/" + repositoryFile.getRelativePath();
                }).collect(ImmutableListCollector.toImmutableList());

                jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(exportJob.getId(), TaskStatus.QUEUED);
                TaskStatusUpdate taskStatusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getTransferTaskOutput(fileUris));
                jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", ImmutableList.of(taskStatusUpdate));
            }
        });

        jobScenarioTools.waitUntilJobHasStatus(exportJob.getId(), JobStatus.COMPLETED);

        // validate job
        Job completedExportJob = jobManager.getJob(exportJob.getId()).get();
        assertThat(completedExportJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    private TransferTaskOutput getTransferTaskOutput(List<String> fileUris) {
        List<ContentInfo> contentInfos = fileUris.stream().map(fileUri -> new ContentInfo(getUriFromString(fileUri), 1024, ImmutableList.of(new Hash("MD5", "abc-def")))).collect(
                ImmutableListCollector.toImmutableList());
        return new TransferTaskOutput(contentInfos);
    }

    private URI getUriFromString(String uri) {
        return Throwables.returnableInstance(() -> new URI(uri));
    }

    private String getRepositoryId(JobScenario.HLSExportJobInfo exportJobInfo) {
        return jobScenarioTools.getRepositoryByName(exportJobInfo.getRepositoryName()).getId();
    }
}
