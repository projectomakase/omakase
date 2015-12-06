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
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileHash;
import org.projectomakase.omakase.content.VariantFileSearchBuilder;
import org.projectomakase.omakase.content.VariantFileType;
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.pipeline.JobScenario.HLSIngestJobInfo;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.search.DefaultSearchBuilder;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.manifest.Manifest;
import org.projectomakase.omakase.task.providers.manifest.ManifestFile;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskOutput;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.assertj.core.groups.Tuple;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.projectomakase.omakase.assertions.OmakaseAssertions.assertThat;
import static org.projectomakase.omakase.assertions.OmakaseAssertions.variantFileComparator;
import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class HLSIngestDelegate {

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


    public void execute(HLSIngestJobInfo ingestJobInfo, Worker worker, Asset asset, Variant variant) {
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

            switch (ingestJobInfo.getHlsManifestType()) {
                case MEDIA:
                    executeMediaManifest(ingestJob, worker, asset, variant, repository);
                    break;
                case MASTER:
                    executeMasterManifest(ingestJob, worker, asset, variant, repository);
                    break;
                default:
                    break;
            }
        }
    }

    private void executeMediaManifest(Job ingestJob, Worker worker, Asset asset, Variant variant, Repository repository) {

        TaskGroup manifestGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
        ManifestTransferTaskOutput output = getManifestTransferTaskOutput(ImmutableList.of(), ImmutableList.of("file0.ts", "file1.ts"));
        jobScenarioTools.consumeTasks(worker, 1, "MANIFEST_TRANSFER", i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, output));
        jobScenarioTools.waitUntilTaskGroupHasStatus(manifestGroup.getId(), TaskStatus.COMPLETED);

        TaskGroup transferGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
        TaskStatusUpdate taskStatusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getTransferTaskOutput(ImmutableList.of("file:/test/file0.ts", "file:/test/file1.ts")));
        jobScenarioTools.consumeTasks(worker, 1, "TRANSFER", ImmutableList.of(taskStatusUpdate));
        jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.COMPLETED);

        // validate job
        ingestJob = jobManager.getJob(ingestJob.getId()).get();
        assertThat(ingestJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        // validate variant repository
        List<VariantRepository> variantRepositories = contentManager.findVariantRepositories(asset.getId(), variant.getId(), new DefaultSearchBuilder().build()).getRecords();
        assertThat(variantRepositories).hasSize(1).extracting("repositoryName", "type").contains(new Tuple(repository.getRepositoryName(), repository.getType()));

        // validate variant files
        ImmutableList.Builder<VariantFile> expectedVariantFilesBuilder = ImmutableList.builder();
        expectedVariantFilesBuilder.add(new VariantFile("manifest.m3u8", 1024L, "manifest.m3u8", "manifest.m3u8", ImmutableList.of(new VariantFileHash("abc-def", "MD5"))));
        expectedVariantFilesBuilder.add(new VariantFile("stream"));

        List<VariantFile> variantFiles = contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords();
        assertThat(variantFiles).hasSize(2).usingElementComparator(variantFileComparator()).containsOnlyElementsOf(expectedVariantFilesBuilder.build());
        validateVirtualVariantFile(variant, variantFiles, "stream", "");

        // validate repository files
        assertThat(repositoryManager.getRepositoryFilesForVariant(repository.getId(), variant.getId())).hasSize(3);
    }

    private void executeMasterManifest(Job ingestJob, Worker worker, Asset asset, Variant variant, Repository repository) {

        TaskGroup masterGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
        jobScenarioTools.consumeTasks(worker, 1, "MANIFEST_TRANSFER",
                i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getManifestTransferTaskOutput(ImmutableList.of("/0/playlist.m3u8", "/1/playlist.m3u8"), ImmutableList.of())));
        jobScenarioTools.waitUntilTaskGroupHasStatus(masterGroup.getId(), TaskStatus.COMPLETED);

        TaskGroup playlistGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
        jobScenarioTools.consumeTasks(worker, 2, "MANIFEST_TRANSFER",
                i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getManifestTransferTaskOutput(ImmutableList.of(), ImmutableList.of("file0.ts", "file1.ts"))));
        jobScenarioTools.waitUntilTaskGroupHasStatus(playlistGroup.getId(), TaskStatus.COMPLETED);

        TaskGroup transferGroup = jobScenarioTools.getQueuedTaskGroupForJob(ingestJob);
        Function<Integer, TaskStatusUpdate> taskStatusUpdateFunction =
                i -> new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100, getTransferTaskOutput(ImmutableList.of("file:/" + i + "/file0.ts", "file:/" + i + "/file1.ts")));
        jobScenarioTools.consumeTasks(worker, 2, "TRANSFER", taskStatusUpdateFunction);
        jobScenarioTools.waitUntilJobHasStatus(ingestJob.getId(), JobStatus.COMPLETED);

        // validate job
        ingestJob = jobManager.getJob(ingestJob.getId()).get();
        assertThat(ingestJob.getStatus()).isEqualTo(JobStatus.COMPLETED);

        // validate variant repository
        List<VariantRepository> variantRepositories = contentManager.findVariantRepositories(asset.getId(), variant.getId(), new DefaultSearchBuilder().build()).getRecords();
        assertThat(variantRepositories).hasSize(1).extracting("name", "repositoryName", "type").contains(new Tuple(repository.getId(), repository.getRepositoryName(), repository.getType()));

        // validate variant files
        ImmutableList.Builder<VariantFile> expectedVariantFilesBuilder = ImmutableList.builder();
        expectedVariantFilesBuilder.add(new VariantFile("manifest.m3u8", 1024L, "manifest.m3u8", "manifest.m3u8", ImmutableList.of(new VariantFileHash("abc-def", "MD5"))));
        expectedVariantFilesBuilder.add(new VariantFile("playlist.m3u8", 1024L, "playlist.m3u8", "0/playlist.m3u8", ImmutableList.of(new VariantFileHash("abc-def", "MD5"))));
        expectedVariantFilesBuilder.add(new VariantFile("playlist.m3u8", 1024L, "playlist.m3u8", "1/playlist.m3u8", ImmutableList.of(new VariantFileHash("abc-def", "MD5"))));
        expectedVariantFilesBuilder.add(new VariantFile("stream-0"));
        expectedVariantFilesBuilder.add(new VariantFile("stream-1"));

        List<VariantFile> variantFiles = contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getRecords();
        assertThat(variantFiles).hasSize(5).usingElementComparator(variantFileComparator()).containsOnlyElementsOf(expectedVariantFilesBuilder.build());
        validateVirtualVariantFile(variant, variantFiles, "stream-0", "0/");
        validateVirtualVariantFile(variant, variantFiles, "stream-1", "1/");

        // validate repository files
        assertThat(repositoryManager.getRepositoryFilesForVariant(repository.getId(), variant.getId())).hasSize(7);
    }

    private void validateVirtualVariantFile(Variant variant, List<VariantFile> variantFiles, String variantFileName, String directory) {
        VariantFile stream0 = variantFiles.stream().filter(variantFile -> variantFile.getVariantFilename().equals(variantFileName)).findFirst().get();
        assertThat(stream0).isVirtualVariantFile();
        ImmutableList.Builder<VariantFile> expectedChildVariantFilesBuilder = ImmutableList.builder();
        expectedChildVariantFilesBuilder.add(new VariantFile("file0.ts", VariantFileType.CHILD, 1024L, "file0.ts", directory + "file0.ts", ImmutableList.of(new VariantFileHash("abc-def", "MD5"))));
        expectedChildVariantFilesBuilder.add(new VariantFile("file1.ts", VariantFileType.CHILD, 1024L, "file1.ts", directory + "file1.ts", ImmutableList.of(new VariantFileHash("abc-def", "MD5"))));
        Set<VariantFile> children = contentManager.getChildVariantFiles(variant.getId(), stream0.getId());
        assertThat(children).hasSize(2).usingElementComparator(variantFileComparator()).containsOnlyElementsOf(expectedChildVariantFilesBuilder.build());
    }

    private static IngestJobConfiguration getIngestJobConfig(HLSIngestJobInfo ingestJobInfo, Variant variant, Repository repository) {
        return IngestJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setRepositories(ImmutableList.of(repository.getId()));
            config.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(ingestJobFile -> {
                ingestJobFile.setUri("file:/test/manifest.m3u8");
                ingestJobFile.setSize(1024L);
                ingestJobFile.setIsManifest(true);
            })));
        });
    }

    private static ManifestTransferTaskOutput getManifestTransferTaskOutput(ImmutableList<String> manifestsUris, ImmutableList<String> fileUris) {
        List<Manifest> manifests =
                IntStream.range(0, manifestsUris.size()).mapToObj(i -> new Manifest(getUriFromString(manifestsUris.get(i)), "stream-" + i)).collect(ImmutableListCollector.toImmutableList());
        List<ManifestFile> files = fileUris.stream().map(uri -> new ManifestFile(getUriFromString(uri), -1)).collect(toImmutableList());
        return new ManifestTransferTaskOutput(manifests, files, 1024, ImmutableList.of(new Hash("MD5", "abc-def")));
    }

    private static TransferTaskOutput getTransferTaskOutput(ImmutableList<String> fileUris) {
        List<ContentInfo> contentInfos = fileUris.stream().map(fileUri -> new ContentInfo(getUriFromString(fileUri), 1024, ImmutableList.of(new Hash("MD5", "abc-def")))).collect(toImmutableList());
        return new TransferTaskOutput(contentInfos);
    }

    private static URI getUriFromString(String uri) {
        return Throwables.returnableInstance(() -> new URI(uri));
    }

}
