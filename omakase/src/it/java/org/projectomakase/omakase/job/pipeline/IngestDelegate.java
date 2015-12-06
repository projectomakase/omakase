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
import org.projectomakase.omakase.broker.BrokerManager;
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
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.search.DefaultSearchBuilder;
import org.projectomakase.omakase.search.SortOrder;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskOutput;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskOutput;
import org.projectomakase.omakase.task.providers.hash.HashTaskOutput;
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
public class IngestDelegate {

    @Inject
    JobManager jobManager;
    @Inject
    TaskManager taskManager;
    @Inject
    BrokerManager brokerManager;
    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    JobScenarioTools jobScenarioTools;


    public void execute(JobScenario.IngestJobInfo ingestJobInfo, Worker worker, Asset asset, Variant variant) {
        Job ingestJob;
        if (ingestJobInfo == null) {
            throw new IllegalArgumentException("an ingest job must be specified");
        } else {
            Repository repository = jobScenarioTools.getRepositoryByName(ingestJobInfo.getRepositoryName());

            ingestJob = jobManager.createJob(Job.Builder.build(j -> {
                j.setJobType(JobType.INGEST);
                j.setStatus(JobStatus.QUEUED);
                j.setJobConfiguration(getIngestJobConfig(ingestJobInfo, variant, repository));
            }));

            jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(ingestJob.getId(), TaskStatus.QUEUED);

            if ("S3".equals(repository.getType())) {
                executeS3IngestJob(ingestJobInfo, worker, asset, variant, ingestJob, repository);
            } else if ("GLACIER".equals(repository.getType())) {
                executeGlacierIngestJob(ingestJobInfo, worker, asset, variant, ingestJob, repository);
            } else {
                executeIngestJob(ingestJobInfo, worker, asset, variant, ingestJob, repository);
            }
        }
    }

    private IngestJobConfiguration getIngestJobConfig(JobScenario.IngestJobInfo ingestJobInfo, Variant variant, Repository repository) {
        return IngestJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setRepositories(ImmutableList.of(repository.getId()));
            config.setIngestJobFiles(IntStream.range(0, ingestJobInfo.getNumberOfFiles()).mapToObj(i -> IngestJobFile.Builder.build(ingestJobFile -> {
                ingestJobFile.setUri("file:/test/test-file" + i + ".txt");
                ingestJobFile.setSize(1024L);
            })).collect(ImmutableListCollector.toImmutableList()));
        });
    }

    private void executeIngestJob(JobScenario.IngestJobInfo ingestJobInfo, Worker worker, Asset asset, Variant variant, Job ingestJob, Repository repository) {

        if (JobScenario.JobFailureType.TRANSFER.equals(ingestJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.FAILED);
        } else {
            jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "TRANSFER", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, new TransferTaskOutput(ImmutableList
                    .of(new ContentInfo(Throwables.returnableInstance(() -> new URI("file:/test/test-file" + i + ".txt")), 1024, Collections.singletonList(new Hash("MD5", "abc-def")))))));
            jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.COMPLETED);
        }
        validate(ingestJobInfo, asset, variant, ingestJob, repository);
    }

    private void executeS3IngestJob(JobScenario.IngestJobInfo ingestJobInfo, Worker worker, Asset asset, Variant variant, Job ingestJob, Repository repository) {

        if (JobScenario.JobFailureType.MULTIPART_PREPARE.equals(ingestJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "HASH", i -> new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.FAILED);
        } else {
            TaskGroup taskGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
            jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "HASH",
                    i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getHashTaskOutput(Hashes.SHA256, Hashes.MD5_BASE64)));
            jobScenarioTools.waitUntilTaskGroupHasStatus(taskGroup.getId(), TaskStatus.COMPLETED);

            if (JobScenario.JobFailureType.TRANSFER.equals(ingestJobInfo.getJobFailureType())) {
                jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "S3_UPLOAD", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
                jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.FAILED);
            } else {
                jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "S3_UPLOAD", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100,
                        new S3UploadTaskOutput(Collections.singletonList(new S3Part(i + 1, "abc")), Collections.singletonList(new Hash("MD5", "abc-def")))));
                jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.COMPLETED);
            }
        }

        validate(ingestJobInfo, asset, variant, ingestJob, repository);
    }

    private void executeGlacierIngestJob(JobScenario.IngestJobInfo ingestJobInfo, Worker worker, Asset asset, Variant variant, Job ingestJob, Repository repository) {
        if (JobScenario.JobFailureType.MULTIPART_PREPARE.equals(ingestJobInfo.getJobFailureType())) {
            jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "HASH", i -> new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed", 0));
            jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.FAILED);
        } else {
            TaskGroup taskGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
            jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "HASH",
                    i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getHashTaskOutput(Hashes.SHA256, Hashes.TREE_HASH)));
            jobScenarioTools.waitUntilTaskGroupHasStatus(taskGroup.getId(), TaskStatus.COMPLETED);

            if (JobScenario.JobFailureType.TRANSFER.equals(ingestJobInfo.getJobFailureType())) {
                jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "GLACIER_UPLOAD", i -> new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed", 0));
                jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.FAILED);
            } else {
                jobScenarioTools.consumeTasks(worker, ingestJobInfo.getNumberOfFiles(), "GLACIER_UPLOAD",
                        i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, new GlacierUploadTaskOutput(Collections.singletonList(new Hash("MD5", "abc-def")))));
                jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.COMPLETED);
            }
        }

        validate(ingestJobInfo, asset, variant, ingestJob, repository);
    }

    private HashTaskOutput getHashTaskOutput(String... algorithms) {
        List<Hash> hashes = Arrays.stream(algorithms).map(algorithm -> new Hash(algorithm, "c72bed74619a0238b707f13fd6ee8bec14cc7779caca89248350ebc1ac9725ec", 0, 1024L)).collect(
                ImmutableListCollector.toImmutableList());
        return new HashTaskOutput(hashes);
    }

    private void validate(JobScenario.IngestJobInfo ingestJobInfo, Asset asset, Variant variant, Job ingestJob, Repository repository) {
        if (JobScenario.JobFailureType.NONE.equals(ingestJobInfo.getJobFailureType())) {
            validateSuccessfulIngest(ingestJobInfo, asset, variant, ingestJob, repository);
        } else {
            validateFailedIngestJob(variant, ingestJob, repository);
        }
    }

    private void validateSuccessfulIngest(JobScenario.IngestJobInfo ingestJobInfo, Asset asset, Variant variant, Job ingestJob, Repository repository) {
        // validate job
        ingestJob = jobManager.getJob(ingestJob.getId()).get();
        assertThat(ingestJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        // validate variant repository
        List<VariantRepository> variantRepositories = contentManager.findVariantRepositories(asset.getId(), variant.getId(), new DefaultSearchBuilder().build()).getRecords();
        assertThat(variantRepositories).hasSize(1).extracting("repositoryName", "type").contains(new Tuple(repository.getRepositoryName(), repository.getType()));

        // validate variant files
        List<VariantFile> variantFiles =
                contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().orderBy(VariantFile.ORIGINAL_FILENAME).sortOrder(SortOrder.ASC).build()).getRecords();
        IntStream.range(0, ingestJobInfo.getNumberOfFiles()).forEach(i -> {
            String originalFileName = "test-file" + i + ".txt";
            assertThat(variantFiles.get(i)).isEqualToComparingOnlyGivenFields(new VariantFile(originalFileName, 1024L, originalFileName, "", null), "size", "originalFilename");
            assertThat(variantFiles.get(i).getHashes()).hasSize(1).extracting("hash", "hashAlgorithm").containsExactly(new Tuple("abc-def", "MD5"));
        });

        // validate repository files
        IntStream.range(0, ingestJobInfo.getNumberOfFiles()).forEach(i -> {
            RepositoryFile expectedRepositoryFile = new RepositoryFile(null, variant.getId(), variantFiles.get(i).getId());
            Assertions.assertThat(repositoryManager.getRepositoryFilesForVariant(repository.getId(), variant.getId())).hasSize(ingestJobInfo.getNumberOfFiles())
                    .usingElementComparatorOnFields("variantId", "variantFileId").contains(expectedRepositoryFile);
        });
    }

    private void validateFailedIngestJob(Variant variant, Job ingestJob, Repository repository) {
        // validate job
        ingestJob = jobManager.getJob(ingestJob.getId()).get();
        assertThat(ingestJob.getStatus()).isEqualTo(JobStatus.FAILED);

        // verify variant repository and file where not created
        assertThat(contentManager.getVariantRepository(variant.getId(), repository.getRepositoryName()).isPresent()).isFalse();
        assertThat(contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords()).isEmpty();
        Assertions.assertThat(repositoryManager.getRepositoryFilesForVariant(repository.getId(), variant.getId())).isEmpty();
    }
}
