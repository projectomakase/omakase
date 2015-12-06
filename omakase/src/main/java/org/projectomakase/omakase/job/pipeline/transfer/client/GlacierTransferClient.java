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
package org.projectomakase.omakase.job.pipeline.transfer.client;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;
import org.projectomakase.omakase.commons.aws.glacier.GlacierUpload;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskConfiguration;
import org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskOutput;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.task.spi.TaskOutput;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;

/**
 * @author Richard Lucas
 */
public class GlacierTransferClient implements TransferClient {

    private static final String GLACIER_UPLOAD = "GLACIER_UPLOAD";

    @Inject
    @Omakase
    GlacierClient glacierClient;
    @Inject
    TaskManager taskManager;

    @Override
    public TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("Glacier Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        GlacierUpload glacierUpload = AWSClients.glacierUploadFromURI(transferFile.getDestination());
        String uploadId = glacierClient
                .initiateMultipartUpload(glacierUpload.getAwsCredentials(), glacierUpload.getRegion(), glacierUpload.getVault(), transferFile.getOriginalFilename(), transferFile.getPartSize().get());
        URI destination = Throwables.returnableInstance(() -> new URI(transferFile.getDestination() + "/multipart-uploads/" + uploadId));
        return TransferFileGroup.builder(transferFileGroup).transferFiles(ImmutableList.of(TransferFile.builder(transferFile).destination(destination).build())).build();
    }

    @Override
    public TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("Glacier Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        GlacierUpload glacierUpload = AWSClients.glacierUploadFromURI(transferFile.getDestination());
        GlacierUploadTaskOutput glacierUploadTaskOutput = (GlacierUploadTaskOutput) taskOutput;

        String archiveHash = Hashes.treeHashFromStrings(transferFile.getParts().stream().map(AWSUploadPart::getPartHash).collect(ImmutableListCollector.toImmutableList())).toString();
        String archiveId = glacierClient
                .completeMultipartUpload(glacierUpload.getAwsCredentials(), glacierUpload.getRegion(), glacierUpload.getVault(), glacierUpload.getUploadId(), transferFile.getSize().get(),
                                         archiveHash);
        TransferFile updatedTransferFile = TransferFile.builder(transferFile).outputHashes(glacierUploadTaskOutput.getHashes()).archiveId(archiveId).build();
        return TransferFileGroup.builder(transferFileGroup).transferFiles(ImmutableList.of(updatedTransferFile)).build();
    }

    @Override
    public void abortTransferFileGroup(TransferFileGroup transferFileGroup) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("Glacier Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        GlacierUpload glacierUpload = AWSClients.glacierUploadFromURI(transferFile.getDestination());
        glacierClient.abortMultipartUpload(glacierUpload.getAwsCredentials(), glacierUpload.getRegion(), glacierUpload.getVault(), glacierUpload.getUploadId());
    }

    @Override
    public Task createTask(TransferFileGroup transferFileGroup, TaskGroup taskGroup, String description, int priority) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("Glacier Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        TaskConfiguration configuration =
                new GlacierUploadTaskConfiguration(transferFile.getSource(), transferFile.getDestination(), transferFile.getPartSize().get(), transferFile.getParts(), ImmutableList.of("MD5"));
        return taskManager.createTask(taskGroup, new Task(GLACIER_UPLOAD, description, priority, configuration));
    }
}
