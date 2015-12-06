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
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantRepositorySearchBuilder;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.ReplicationJobConfiguration;
import org.projectomakase.omakase.job.pipeline.JobScenario.HLSReplicationJobInfo;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.search.SortOrder;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.assertj.core.groups.Tuple;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class HLSReplicationDelegate {

    @Inject
    JobManager jobManager;
    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    JobScenarioTools jobScenarioTools;


    public void execute(HLSReplicationJobInfo replicationJobInfo, Worker worker, Variant variant) {
        Repository sourceRepository = jobScenarioTools.getRepositoryByName(replicationJobInfo.getSourceRepositoryName());
        Repository destinationRepository = jobScenarioTools.getRepositoryByName(replicationJobInfo.getDestinationRepositoryName());

        Job replicateJob = jobManager.createJob(Job.Builder.build(j -> {
            j.setJobType(JobType.REPLICATION);
            j.setStatus(JobStatus.QUEUED);
            j.setJobConfiguration(getReplicationJobConfig(replicationJobInfo, variant, sourceRepository, destinationRepository));
        }));

        jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(replicateJob.getId(), TaskStatus.QUEUED);

        transfer(replicationJobInfo, worker, variant, replicateJob, sourceRepository, destinationRepository);
    }

    private ReplicationJobConfiguration getReplicationJobConfig(HLSReplicationJobInfo replicationJobInfo, Variant variant, Repository sourceRepository, Repository destinationRepository) {
        return ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setSourceRepositories(ImmutableList.of(sourceRepository.getId()));
            config.setDestinationRepositories(ImmutableList.of(destinationRepository.getId()));
        });
    }


    private void transfer(HLSReplicationJobInfo replicationJobInfo, Worker worker, Variant variant, Job replicateJob, Repository sourceRepository, Repository destinationRepository) {
        URI sourceRepositoryUri = repositoryManager.getRepositoryUri(sourceRepository.getId());

        List<VariantFile> variantFiles = contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords();
        variantFiles.forEach(variantFile -> {
            if (variantFile.getOriginalFilename() != null) {
                RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(sourceRepository.getId(), variantFile.getId()).get();
                String contentInfoUri = sourceRepositoryUri + "/" + repositoryFile.getRelativePath();
                jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100,
                        new TransferTaskOutput(ImmutableList.of(new ContentInfo(getUriFromString(contentInfoUri), 1024, Collections.singletonList(new Hash("MD5", "abc-def")))))));
            } else {
                Set<VariantFile> childVariantFiles = contentManager.getChildVariantFiles(variant.getId(), variantFile.getId());
                List<String> fileUris = childVariantFiles.stream().map(childVariantFile -> {
                    RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(sourceRepository.getId(), childVariantFile.getId()).get();
                    return sourceRepositoryUri + "/" + repositoryFile.getRelativePath();
                }).collect(ImmutableListCollector.toImmutableList());

                TaskStatusUpdate taskStatusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getTransferTaskOutput(fileUris));
                jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", ImmutableList.of(taskStatusUpdate));
            }
        });

        jobScenarioTools.waitUntilJobHasStatus(replicateJob.getId(), JobStatus.COMPLETED);

        // validate job
        replicateJob = jobManager.getJob(replicateJob.getId()).get();
        assertThat(replicateJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        // validate variant repository
        List<VariantRepository> variantRepositories =
                contentManager.findVariantRepositories(variant.getId(), new VariantRepositorySearchBuilder().orderBy(VariantRepository.CREATED).sortOrder(SortOrder.ASC).build())
                        .getRecords();
        assertThat(variantRepositories).hasSize(2).extracting("repositoryName", "type")
                .contains(new Tuple(sourceRepository.getRepositoryName(), sourceRepository.getType()), new Tuple(destinationRepository.getRepositoryName(), destinationRepository.getType()));

        // validate repository files
        int expectedRepositoryFileSize = repositoryManager.getRepositoryFilesForVariant(sourceRepository.getId(), variant.getId()).size();
        assertThat(repositoryManager.getRepositoryFilesForVariant(destinationRepository.getId(), variant.getId())).hasSize(expectedRepositoryFileSize);
    }

    private TransferTaskOutput getTransferTaskOutput(List<String> fileUris) {
        List<ContentInfo> contentInfos = fileUris.stream().map(fileUri -> new ContentInfo(getUriFromString(fileUri), 1024, ImmutableList.of(new Hash("MD5", "abc-def")))).collect(
                ImmutableListCollector.toImmutableList());
        return new TransferTaskOutput(contentInfos);
    }

    private URI getUriFromString(String uri) {
        return Throwables.returnableInstance(() -> new URI(uri));
    }
}
