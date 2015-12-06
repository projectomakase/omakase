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
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileSearchBuilder;
import org.projectomakase.omakase.content.VariantFileType;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.configuration.ExportJobConfiguration;
import org.projectomakase.omakase.job.pipeline.MultipartUploadInfoProvider;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.search.SearchResult;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Pipeline Stage used to prepare an export.
 *
 * @author Richard Lucas
 */
public class ExportPrepareStage implements PipelineStage {

    @Inject
    JobManager jobManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    LocationManager locationManager;
    @Inject
    ContentManager contentManager;
    @Inject
    MultipartUploadInfoProvider multipartUploadInfoProvider;

    @Override
    public PipelineStageResult prepare(PipelineContext pipelineContext) {

        String pipelineId = pipelineContext.getPipelineId();

        String jobId = pipelineContext.getObjectId();
        Job job = jobManager.getJob(jobId).orElseThrow(() -> new OmakaseRuntimeException("Unable to find job " + jobId + " for pipeline " + pipelineId));
        ExportJobConfiguration configuration = (ExportJobConfiguration) job.getJobConfiguration();
        String variantId = configuration.getVariant();
        String repositoryId = configuration.getRepositories().get(0);

        ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();
        propertiesBuilder.put("priority", Long.toString(job.getPriority()));
        propertiesBuilder.put("variant", variantId);
        propertiesBuilder.put("sourceRepositoryId", repositoryId);
        propertiesBuilder.put("transferType", "EXPORT");

        URI repositoryUri = repositoryManager.getRepositoryUri(repositoryId);
        String destinationLocation = configuration.getLocations().stream().findFirst().get();

        SearchResult<VariantFile> searchResult = contentManager.findVariantFiles(variantId, new VariantFileSearchBuilder().count(-1).build());
        List<TransferFileGroup> transferFileGroups =
                searchResult.getRecords().stream().map(variantFile -> createTransferFileGroup(variantId, repositoryId, repositoryUri, destinationLocation, variantFile)).collect(
                        ImmutableListCollector.toImmutableList());
        Transfer transfer = new Transfer(transferFileGroups);
        propertiesBuilder.put(TransferPipeline.TRANSFER, transfer.toJson());

        multipartUploadInfoProvider.get(locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI(destinationLocation))))
                .ifPresent(multipartUploadInfo -> propertiesBuilder.put("multipartUploadInfo", multipartUploadInfo.toJson()));

        if (!TransferPipeline.doesTransferHaveFiles(transfer)) {
            return PipelineStageResult.builder(pipelineId, PipelineStageStatus.FAILED)
                    .addProperties(propertiesBuilder.build())
                    .addMessages(ImmutableSet.of("Failed to find any files to export"))
                    .build();
        } else {
            return PipelineStageResult.builder(pipelineId, PipelineStageStatus.COMPLETED)
                    .addProperties(propertiesBuilder.build())
                    .build();
        }
    }

    private TransferFileGroup createTransferFileGroup(String variantId, String repositoryId, URI repositoryUri, String destinationLocation, VariantFile variantFile) {
        if (VariantFileType.VIRTUAL.equals(variantFile.getType())) {
            Set<VariantFile> childVariantFiles = contentManager.getChildVariantFiles(variantId, variantFile.getId());

            List<TransferFile> transferFiles = childVariantFiles.stream()
                    .map(childVariantFile -> createTransferFileForChildVariantFile(repositoryUri, repositoryManager.getRepositoryFileForVariantFile(repositoryId, childVariantFile.getId()).get(),
                                                                                   destinationLocation,
                                                                                   childVariantFile)).collect(ImmutableListCollector.toImmutableList());

            return TransferFileGroup.builder().description(variantFile.getVariantFilename()).transferFiles(transferFiles).build();
        } else {
            RepositoryFile repositoryFile = repositoryManager.getRepositoryFileForVariantFile(repositoryId, variantFile.getId()).get();

            return TransferFileGroup.builder().transferFiles(ImmutableList.of(createTransferFile(variantId, repositoryUri, repositoryFile, destinationLocation))).build();
        }
    }

    private TransferFile createTransferFile(String variantId, URI repositoryUri, RepositoryFile repositoryFile, String destinationLocation) {
        URI source = Throwables.returnableInstance(() -> new URI(repositoryUri.toString() + "/" + repositoryFile.getRelativePath()));
        VariantFile variantFile = contentManager.getVariantFile(variantId, repositoryFile.getVariantFileId())
                .orElseThrow(() -> new NotFoundException("Unable to find variant file " + repositoryFile.getVariantFileId()));
        URI destination = locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI(destinationLocation + "/" + variantFile.getOriginalFilename())));
        return TransferFile.builder().originalFilename(variantFile.getOriginalFilename()).source(source).destination(destination).size(variantFile.getSize())
                .sourceRepositoryFileId(repositoryFile.getId()).build();

    }


    private TransferFile createTransferFileForChildVariantFile(URI repositoryUri, RepositoryFile repositoryFile, String destinationDir, VariantFile childVariantFile) {
        URI source = Throwables.returnableInstance(() -> new URI(repositoryUri.toString() + "/" + repositoryFile.getRelativePath()));

        URI destination = Throwables.returnableInstance(() -> new URI(destinationDir + "/" + childVariantFile.getOriginalFilepath()));
        return TransferFile.builder().originalFilename(childVariantFile.getOriginalFilename()).source(source).destination(destination).size(childVariantFile.getSize())
                .sourceRepositoryFileId(repositoryFile.getId()).build();

    }

    @Override
    public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
        throw new UnsupportedOperationException();
    }
}
