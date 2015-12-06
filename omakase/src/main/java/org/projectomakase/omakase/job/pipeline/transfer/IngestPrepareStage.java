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
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.configuration.ManifestType;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;

import javax.inject.Inject;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;

/**
 * Pipeline stage used to prepare an ingest.
 *
 * @author Richard Lucas
 */
public class IngestPrepareStage implements PipelineStage {

    @Inject
    JobManager jobManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    LocationManager locationManager;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {

        String pipelineId = pipelineContext.getPipelineId();
        String jobId = pipelineContext.getObjectId();

        Job job = jobManager.getJob(jobId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find job " + jobId + " for pipeline " + pipelineId));

        IngestJobConfiguration configuration = (IngestJobConfiguration) job.getJobConfiguration();

        String variantId = configuration.getVariant();
        String repositoryId = configuration.getRepositories().get(0);

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();
        propertiesBuilder.put("priority", Long.toString(job.getPriority()));
        propertiesBuilder.put("variant", variantId);
        propertiesBuilder.put("destinationRepositoryId", repositoryId);
        propertiesBuilder.put("transferType", "INGEST");

        Repository repository = repositoryManager.getRepository(repositoryId).orElseThrow(() -> new OmakaseRuntimeException("Repository " + repositoryId + " does not exist"));
        URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);

        // determine if the multipart transfer prepare stage is required prior to transferring
        getMultipartUploadInfo(repositoryId).ifPresent(multipartUploadInfo -> propertiesBuilder.put("multipartUploadInfo", multipartUploadInfo.toJson()));

        Optional<ManifestType> manifestType = configuration.getManifestType();
        if (manifestType.isPresent()) {
            propertiesBuilder.put("manifestType", manifestType.get().name());
            propertiesBuilder.put(TransferPipeline.MANIFEST_TRANSFER, createManifestTransfer(manifestType.get(), configuration.getIngestJobFiles(), variantId, repository, repositoryUri).toJson());
            propertiesBuilder.put(TransferPipeline.TRANSFER, new Transfer(ImmutableList.of()).toJson());
        } else {
            validateFileSizeIsPresent(configuration, repository);
            propertiesBuilder.put(TransferPipeline.TRANSFER, createTransfer(configuration.getIngestJobFiles(), variantId, repositoryId, repositoryUri).toJson());
        }
        return PipelineStageResult.builder(pipelineId, PipelineStageStatus.COMPLETED).addProperties(propertiesBuilder.build()).build();
    }

    private ManifestTransfer createManifestTransfer(ManifestType manifestType, List<IngestJobFile> ingestJobFiles, String variantId, Repository repository, URI repositoryUri) {
        List<ManifestTransferFile> manifestTransferFiles =
                ingestJobFiles.stream().map(ingestJobFile -> createManifestTransferFile(ingestJobFile, variantId, repository, repositoryUri)).collect(toImmutableList());

        URI manifestUri = Throwables.returnableInstance(
                () -> new URI(ingestJobFiles.stream().map(IngestJobFile::getUri).findFirst().orElseThrow(() -> new OmakaseRuntimeException("missing expected ingest job file URI"))));

        Path rootPath = Optional.ofNullable(Paths.get(manifestUri.getPath()).getParent()).orElse(Paths.get("/"));
        return new ManifestTransfer(rootPath, manifestType, manifestTransferFiles);
    }

    private ManifestTransferFile createManifestTransferFile(IngestJobFile ingestJobFile, String variantId, Repository repository, URI repositoryUri) {
        RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, repository.getId());
        URI source = locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI(ingestJobFile.getUri())));
        String originalFileName = source.toString().replaceFirst(".*/([^/?]+).*", "$1");
        URI destination = TransferPipeline.getDestinationUri(repository.getType(), repositoryUri, repositoryFile.getRelativePath(), originalFileName);
        ManifestTransferFile.Builder builder =
                ManifestTransferFile.builder().repositoryFileId(repositoryFile.getId()).originalFilename(originalFileName).originalFilepath(originalFileName).source(source).destination(destination);
        if (ingestJobFile.getSize() != null) {
            builder.size(ingestJobFile.getSize());
        }
        return builder.build();

    }

    private Transfer createTransfer(List<IngestJobFile> ingestJobFiles, String variantId, String repositoryId, URI repositoryUri) {
        return new Transfer(createTransferFileGroups(ingestJobFiles, variantId, repositoryId, repositoryUri));
    }

    private List<TransferFileGroup> createTransferFileGroups(List<IngestJobFile> ingestJobFiles, String variantId, String repositoryId, URI repositoryUri) {
        return ingestJobFiles.stream()
                .map(ingestJobFile -> TransferFileGroup.builder()
                        .transferFiles(ImmutableList.of(createTransferFile(ingestJobFile, variantId, repositoryId, repositoryUri)))
                        .build())
                .collect(toImmutableList());
    }

    private TransferFile createTransferFile(IngestJobFile ingestJobFile, String variantId, String repositoryId, URI repositoryUri) {
        RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, repositoryId);
        URI source = locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI(ingestJobFile.getUri())));
        URI destination = Optional.ofNullable(repositoryFile.getRelativePath()).map(relativePath -> Throwables.returnableInstance(() -> new URI(repositoryUri.toString() + "/" + relativePath)))
                .orElse(repositoryUri);
        String originalFileName = source.toString().replaceFirst(".*/([^/?]+).*", "$1");
        TransferFile.Builder builder = TransferFile.builder().destinationRepositoryFileId(repositoryFile.getId()).originalFilename(originalFileName).source(source).destination(destination);
        if (ingestJobFile.getSize() != null) {
            builder.size(ingestJobFile.getSize());
        }
        return builder.build();

    }

    private void validateFileSizeIsPresent(IngestJobConfiguration configuration, Repository repository) {
        // We do not currently have a mechanism to get the file size pre-ingest and need to know it when ingesting into Glacier and S3 in order to split the files into parts. In the future this
        // requirement will be relaxed as we will support retrieving the file size prior to splitting the file into parts.

        if ("S3".equals(repository.getType()) || "GLACIER".equals(repository.getType())) {
            configuration.getIngestJobFiles().stream().filter(ingestJobFile -> ingestJobFile.getSize() == null).findAny().ifPresent(ingestJobFile -> {
                throw new InvalidPropertyException("File size is required when ingesting into S3 or Glacier");
            });
        }
    }

    private Optional<MultipartUploadInfo> getMultipartUploadInfo(String repositoryId) {
        if (repositoryManager.doesRepositoryRequireMultipartUpload(repositoryId)) {
            return Optional.of(repositoryManager.getMultipartUploadInfoForRepository(repositoryId));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        throw new UnsupportedOperationException();
    }
}
