/*
 * #%L
 * omakase-tool-s3
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
package org.projectomakase.omakase.worker.tool.s3;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.s3.S3Client;
import org.projectomakase.omakase.commons.aws.s3.S3Part;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskConfiguration;
import org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskOutput;
import org.projectomakase.omakase.worker.Omakase;
import org.projectomakase.omakase.worker.tool.Tool;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.ToolException;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerResolver;
import org.jboss.logging.Logger;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Richard Lucas
 */
@Named(S3UploadTool.NAME)
public class S3UploadTool implements Tool {

    private static final Logger LOGGER = Logger.getLogger(S3UploadTool.class);

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;
    @Inject
    @Omakase
    S3Client s3Client;
    @Inject
    Event<ToolCallback> event;

    public static final String NAME = "S3_UPLOAD";

    @Override
    public void execute(Task task) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received task " + task.toString());
        }

        String taskId = task.getId();

        try {
            S3UploadTaskConfiguration configuration = (S3UploadTaskConfiguration) task.getConfiguration();
            checkArgument(configuration.getSource() != null, "source uri can not be null");
            checkArgument(configuration.getDestination() != null, "destination uri can not be null");
            checkArgument(configuration.getPartSize() > 0, "part size must be greater than 0");
            checkArgument(!configuration.getParts().isEmpty(), "parts must not be empty");

            //TODO handle multiple hashes. for now we always generate MD5 hash

            try (ProtocolHandler sourceProtocolHandler = protocolHandlerResolver.getProtocolHandler(configuration.getSource())) {

                upload(taskId, sourceProtocolHandler, configuration);
            }

        } catch (Exception e) {
            LOGGER.error("GlacierUploadTool failed. Reason: " + e.getMessage(), e);
            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed to copy file. Reason: " + e.getMessage(), 0)));
        }
    }

    private void upload(String taskId, ProtocolHandler sourceProtocolHandler, S3UploadTaskConfiguration configuration) throws IOException {
        URI sourceUri = configuration.getSource();
        URI destinationUri = configuration.getDestination();
        long partSize = configuration.getPartSize();
        List<AWSUploadPart> uploadParts = configuration.getParts();

        sourceProtocolHandler.init(sourceUri);

        S3Upload upload = AWSClients.s3UploadFromURI(destinationUri);

        try (InputStream inputStream = sourceProtocolHandler.openStream(); HashingInputStream hashingInputStream = new HashingInputStream(Hashing.md5(), inputStream)) {
            List<S3Part> s3Parts = uploadParts.stream().map(uploadPart -> uploadPart(hashingInputStream, upload, partSize, uploadPart)).collect(ImmutableListCollector.toImmutableList());
            String md5 = hashingInputStream.hash().toString();

            S3UploadTaskOutput taskOutput = new S3UploadTaskOutput(s3Parts, ImmutableList.of(new Hash("MD5", md5)));
            String message = "Uploaded " + sourceUri + " to " + upload.getHost() + upload.getEndpoint() + ". Calculated md5: " + md5;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message);
            }
            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.COMPLETED, message, 100, taskOutput)));
        }
    }

    private S3Part uploadPart(InputStream source, S3Upload upload, long partSize, AWSUploadPart uploadPart) {
        try {
            return s3Client.uploadMultipartPart(upload, uploadPart, partSize, source);
        } catch (Exception e) {
            String message = "Upload Failed. Reason: " + e.getMessage();
            LOGGER.error(message, e);
            throw new ToolException(message, e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
