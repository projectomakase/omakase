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
import org.projectomakase.omakase.commons.aws.s3.S3Part;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.content.Asset;
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
import org.projectomakase.omakase.job.pipeline.JobScenario.JobFailureType;
import org.projectomakase.omakase.job.pipeline.JobScenario.ReplicationJobInfo;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.search.SortOrder;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskOutput;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskOutput;
import org.projectomakase.omakase.task.providers.hash.HashTaskOutput;
import org.projectomakase.omakase.task.providers.restore.RestoreTaskOutput;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class ReplicationDelegate {

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

    public void execute(ReplicationJobInfo replicationJobInfo, Worker worker, Asset asset, Variant variant) {
        Repository sourceRepository = jobScenarioTools.getRepositoryByName(replicationJobInfo.getSourceRepositoryName());
        Repository destinationRepository = jobScenarioTools.getRepositoryByName(replicationJobInfo.getDestinationRepositoryName());

        Job replicationJob = jobManager.createJob(Job.Builder.build(j -> {
            j.setJobType(JobType.REPLICATION);
            j.setStatus(JobStatus.QUEUED);
            j.setJobConfiguration(getReplicationJobConfiguration(replicationJobInfo, variant, sourceRepository, destinationRepository));
        }));

        jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(replicationJob.getId(), TaskStatus.QUEUED);

        int numberOfFiles = Ints.checkedCast(contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getTotalRecords());


        if (JobFailureType.EMPTY_REPOSITORY.equals(replicationJobInfo.getJobFailureType())) {
            replicationJob = jobManager.getJob(replicationJob.getId()).get();
            assertThat(replicationJob.getStatus()).isEqualTo(JobStatus.FAILED);
        } else {
            if (restore(replicationJobInfo, replicationJob, sourceRepository)) {

                if (multipartPrepare(replicationJobInfo, worker, replicationJob, destinationRepository, numberOfFiles)) {
                    if ("S3".equals(destinationRepository.getType())) {
                        s3Upload(replicationJobInfo, worker, replicationJob, numberOfFiles);
                    } else if ("GLACIER".equals(destinationRepository.getType())) {
                        glacierUpload(replicationJobInfo, worker, replicationJob, numberOfFiles);
                    } else {
                        transfer(replicationJobInfo, worker, variant, replicationJob, numberOfFiles);
                    }

                }
                validate(replicationJobInfo, replicationJob, asset, variant, sourceRepository, destinationRepository, numberOfFiles);
            }
        }
    }

    private boolean restore(ReplicationJobInfo replicationJobInfo, Job replicationJob, Repository sourceRepository) {
        if (repositoryManager.doesRepositoryRequireRestore(sourceRepository.getId())) {
            TaskGroup taskGroup = taskManager.getTaskGroups(replicationJob.getId()).stream().findFirst().get();
            if (JobFailureType.RESTORE.equals(replicationJobInfo.getJobFailureType())) {
                taskManager.getTasks(taskGroup.getId()).forEach(task -> taskManager.updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed", 0)));
                jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.FAILED);
                return false;
            } else {
                taskManager.getTasks(taskGroup.getId()).forEach(task -> taskManager
                        .updateTaskStatus(task.getId(), new TaskStatusUpdate(TaskStatus.COMPLETED, "Succeeded", 100, new RestoreTaskOutput(Throwables.returnableInstance(() -> new URI("test-archive-id"))))));
                jobScenarioTools.waitUntilTaskGroupHasStatus(taskGroup.getId(), TaskStatus.COMPLETED);
                return true;
            }
        }
        return true;
    }

    private boolean multipartPrepare(ReplicationJobInfo replicationJobInfo, Worker worker, Job replicationJob, Repository destintationRepository, int numberOfFiles) {
        if (repositoryManager.doesRepositoryRequireMultipartUpload(destintationRepository.getId())) {
            if (JobFailureType.MULTIPART_PREPARE.equals(replicationJobInfo.getJobFailureType())) {
                jobScenarioTools.consumeTasks(worker, numberOfFiles, "HASH", i -> new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed", 0));
                jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.FAILED);
                return false;
            } else {
                HashTaskOutput taskOutput;
                if ("S3".equals(destintationRepository.getType())) {
                    taskOutput = getHashTaskOutput(Hashes.SHA256, Hashes.MD5_BASE64);
                } else {
                    taskOutput = getHashTaskOutput(Hashes.SHA256, Hashes.TREE_HASH);
                }
                TaskGroup taskGroup = jobScenarioTools.getQueuedTaskGroupForJob(replicationJob);
                jobScenarioTools.consumeTasks(worker, numberOfFiles, "HASH", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, taskOutput));
                jobScenarioTools.waitUntilTaskGroupHasStatus(taskGroup.getId(), TaskStatus.COMPLETED);
                return true;
            }
        }
        return true;
    }

    private HashTaskOutput getHashTaskOutput(String... algorithms) {
        List<Hash> hashes = Arrays.stream(algorithms).map(algorithm -> new Hash(algorithm, "c72bed74619a0238b707f13fd6ee8bec14cc7779caca89248350ebc1ac9725ec", 0, 1024L)).collect(
                ImmutableListCollector.toImmutableList());
        return new HashTaskOutput(hashes);
    }

    private void transfer(ReplicationJobInfo replicationJobInfo, Worker worker, Variant variant, Job replicationJob, int numberOfFiles) {
        if (JobFailureType.TRANSFER.equals(replicationJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, numberOfFiles, "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.FAILED);
        } else {
            Repository sourceRepository = jobScenarioTools.getRepositoryByName(replicationJobInfo.getSourceRepositoryName());
            URI sourceRepositoryUri = repositoryManager.getRepositoryUri(sourceRepository.getId());
            List<VariantFile> variantFiles = contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords();
            variantFiles.forEach(variantFile -> {
                RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(sourceRepository.getId(), variantFile.getId()).get();
                String contentInfoUri = sourceRepositoryUri + "/" + repositoryFile.getRelativePath();
                jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, new TransferTaskOutput(
                        ImmutableList.of(new ContentInfo(getUriFromString(contentInfoUri), 1024, Collections.singletonList(new Hash("MD5", "abc-def")))))));
            });
            jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.COMPLETED);
        }
    }

    private void s3Upload(ReplicationJobInfo replicationJobInfo, Worker worker, Job replicationJob, int numberOfFiles) {
        if (JobFailureType.TRANSFER.equals(replicationJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, numberOfFiles, "S3_UPLOAD", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.FAILED);
        } else {
            jobScenarioTools.consumeTasks(worker, numberOfFiles, "S3_UPLOAD", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100,
                                                                                                        new S3UploadTaskOutput(Collections.singletonList(new S3Part(i + 1, "abc")),
                                                                                                                               Collections.singletonList(new Hash("MD5", "abc-def")))));
            jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.COMPLETED);
        }
    }

    private void glacierUpload(ReplicationJobInfo replicationJobInfo, Worker worker, Job replicationJob, int numberOfFiles) {
        if (JobFailureType.TRANSFER.equals(replicationJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, numberOfFiles, "GLACIER_UPLOAD", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.FAILED);
        } else {
            jobScenarioTools.consumeTasks(worker, numberOfFiles, "GLACIER_UPLOAD",
                                          i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, new GlacierUploadTaskOutput(Collections.singletonList(new Hash("MD5", "abc-def")))));
            jobScenarioTools.waitUntilJobHasStatus(replicationJob.getId(), JobStatus.COMPLETED);
        }
    }

    private void validate(ReplicationJobInfo replicationJobInfo, Job replicationJob, Asset asset, Variant variant, Repository sourceRepository, Repository destinationRepository, int numberOfFiles) {
        // validate job
        replicationJob = jobManager.getJob(replicationJob.getId()).get();
        if (JobFailureType.NONE.equals(replicationJobInfo.getJobFailureType())) {
            validateSuccessfulReplication(asset, variant, replicationJob, sourceRepository, destinationRepository, numberOfFiles);
        } else {
            validateFailedReplication(asset, variant, replicationJob, sourceRepository, destinationRepository, numberOfFiles);
        }
    }

    private void validateSuccessfulReplication(Asset asset, Variant variant, Job replicationJob, Repository sourceRepository, Repository destinationRepository, int numberOfFiles) {
        // validate job
        replicationJob = jobManager.getJob(replicationJob.getId()).get();
        assertThat(replicationJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        // validate variant repository
        List<VariantRepository> variantRepositories =
                contentManager.findVariantRepositories(asset.getId(), variant.getId(), new VariantRepositorySearchBuilder().orderBy(VariantRepository.CREATED).sortOrder(SortOrder.ASC).build())
                        .getRecords();
        assertThat(variantRepositories).hasSize(2).extracting("repositoryName", "type")
                .contains(new Tuple(sourceRepository.getRepositoryName(), sourceRepository.getType()), new Tuple(destinationRepository.getRepositoryName(), destinationRepository.getType()));

        // validate variant files
        List<VariantFile> variantFiles =
                contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().orderBy(VariantFile.ORIGINAL_FILENAME).sortOrder(SortOrder.ASC).build()).getRecords();
        IntStream.range(0, numberOfFiles).forEach(i -> {
            String originalFileName = "test-file" + i + ".txt";
            assertThat(variantFiles.get(i)).isEqualToComparingOnlyGivenFields(new VariantFile(originalFileName, 1024L, originalFileName, "", null), "size", "originalFilename");
            Assertions.assertThat(variantFiles.get(i).getHashes()).hasSize(1).extracting("hash", "hashAlgorithm").containsExactly(new Tuple("abc-def", "MD5"));
        });

        // validate repository files
        assertThat(repositoryManager.getRepositoryFilesForVariant(destinationRepository.getId(), variant.getId())).hasSize(numberOfFiles)
                .usingElementComparatorOnFields("variantId", "variantFileId")
                .contains(IntStream.range(0, numberOfFiles).mapToObj(i -> new RepositoryFile(null, variant.getId(), variantFiles.get(i).getId())).toArray(RepositoryFile[]::new));

    }

    private void validateFailedReplication(Asset asset, Variant variant, Job replicationJob, Repository sourceRepository, Repository destinationRepository, int numberOfFiles) {
        // validate job
        replicationJob = jobManager.getJob(replicationJob.getId()).get();
        assertThat(replicationJob.getStatus()).isEqualTo(JobStatus.FAILED);

        // validate variant repository
        List<VariantRepository> variantRepositories =
                contentManager.findVariantRepositories(asset.getId(), variant.getId(), new VariantRepositorySearchBuilder().orderBy(VariantRepository.CREATED).sortOrder(SortOrder.ASC).build())
                        .getRecords();
        assertThat(variantRepositories).hasSize(1).extracting("repositoryName", "type").contains(new Tuple(sourceRepository.getRepositoryName(), sourceRepository.getType()));

        // validate variant files
        List<VariantFile> variantFiles =
                contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().orderBy(VariantFile.ORIGINAL_FILENAME).sortOrder(SortOrder.ASC).build()).getRecords();
        IntStream.range(0, numberOfFiles).forEach(i -> {
            String originalFileName = "test-file" + i + ".txt";
            assertThat(variantFiles.get(i)).isEqualToComparingOnlyGivenFields(new VariantFile(originalFileName, 1024L, originalFileName, "", null), "size", "originalFilename");
            Assertions.assertThat(variantFiles.get(i).getHashes()).hasSize(1).extracting("hash", "hashAlgorithm").containsExactly(new Tuple("abc-def", "MD5"));
        });

        // validate repository files
        assertThat(repositoryManager.getRepositoryFilesForVariant(destinationRepository.getId(), variant.getId())).isEmpty();
    }

    private ReplicationJobConfiguration getReplicationJobConfiguration(ReplicationJobInfo replicationJobInfo, Variant variant, Repository sourceRepository, Repository destinationRepository) {
        return ReplicationJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setSourceRepositories(ImmutableList.of(sourceRepository.getId()));
            config.setDestinationRepositories(ImmutableList.of(destinationRepository.getId()));
        });
    }

    private URI getUriFromString(String uri) {
        return Throwables.returnableInstance(() -> new URI(uri));
    }
}
