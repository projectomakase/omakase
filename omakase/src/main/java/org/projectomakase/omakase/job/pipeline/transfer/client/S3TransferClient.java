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
import org.projectomakase.omakase.commons.aws.s3.S3Client;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskConfiguration;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskOutput;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.task.spi.TaskOutput;

import javax.inject.Inject;
import java.net.URI;

/**
 * AWS S3 {@link TransferClient} implementation
 *
 * @author Richard Lucas
 */
public class S3TransferClient implements TransferClient {

    private static final String S3_UPLOAD = "S3_UPLOAD";

    @Inject
    @Omakase
    S3Client s3Client;
    @Inject
    TaskManager taskManager;

    @Override
    public TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("S3 Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        String uploadId = s3Client.initiateMultipartUpload(AWSClients.s3UploadFromURI(transferFile.getDestination()), transferFile.getOriginalFilename());
        URI destination;
        if (transferFile.getDestination().getQuery() == null) {
            destination = Throwables.returnableInstance(() -> new URI(transferFile.getDestination() + "?uploadId=" + uploadId));
        } else {
            destination = Throwables.returnableInstance(() -> new URI(transferFile.getDestination() + "&uploadId=" + uploadId));
        }
        return TransferFileGroup.builder(transferFileGroup).transferFiles(ImmutableList.of(TransferFile.builder(transferFile).destination(destination).build())).build();
    }

    @Override
    public TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("S3 Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        S3Upload s3Upload = AWSClients.s3UploadFromURI(transferFile.getDestination());
        S3UploadTaskOutput s3UploadtaskOutput = (S3UploadTaskOutput) taskOutput;
        s3Client.completeMultipartUpload(s3Upload, s3UploadtaskOutput.getParts());
        TransferFile updatedTransferFile = TransferFile.builder(transferFile).outputHashes(s3UploadtaskOutput.getHashes()).build();
        return TransferFileGroup.builder(transferFileGroup).transferFiles(ImmutableList.of(updatedTransferFile)).build();

    }

    @Override
    public void abortTransferFileGroup(TransferFileGroup transferFileGroup) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("S3 Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        s3Client.abortMultipartUpload(AWSClients.s3UploadFromURI(transferFile.getDestination()));
    }

    @Override
    public Task createTask(TransferFileGroup transferFileGroup, TaskGroup taskGroup, String description, int priority) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            throw new OmakaseRuntimeException("S3 Transfer client does not support Transfer File Groups with more than one file");
        }
        TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
        TaskConfiguration configuration =
                new S3UploadTaskConfiguration(transferFile.getSource(), transferFile.getDestination(), transferFile.getPartSize().get(), transferFile.getParts(), ImmutableList.of("MD5"));
        return taskManager.createTask(taskGroup, new Task(S3_UPLOAD, description, priority, configuration));
    }
}
