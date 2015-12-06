/*
 * #%L
 * omakase-tool-transfer
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
package org.projectomakase.omakase.worker.tool.transfer;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.projectomakase.omakase.worker.tool.Tool;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerResolver;
import org.jboss.logging.Logger;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Richard Lucas
 */
@Named(TransferTool.NAME)
public class TransferTool implements Tool {

    public static final String NAME = "TRANSFER";

    private static final Logger LOGGER = Logger.getLogger(TransferTool.class);

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;

    @Inject
    Event<ToolCallback> event;

    @Override
    public void execute(Task task) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received task " + task.toString());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(task.getConfiguration());
        }

        String taskId = task.getId();

        try {
            TransferTaskConfiguration configuration = (TransferTaskConfiguration) task.getConfiguration();
            List<ContentInfo> contentInfos = configuration.getIoInstructions().stream().map(this::transfer).collect(ImmutableListCollector.toImmutableList());

            TransferTaskOutput taskOutput = new TransferTaskOutput(contentInfos);

            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.COMPLETED, "Transferred " + configuration.getIoInstructions().size() + " file(s)", 100, taskOutput)));

        } catch (Exception e) {
            LOGGER.error("IOTool failed. Reason: " + e.getMessage(), e);
            String message = Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage());
            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed to transfer file. Reason: " + message, 0)));
        }
    }

    private ContentInfo transfer(IOInstruction ioInstruction) {
        checkArgument(ioInstruction.getSource() != null, "source uri can not be null");
        checkArgument(ioInstruction.getDestination() != null, "destination uri can not be null");

        URI sourceUri = ioInstruction.getSource();
        URI destinationUri = ioInstruction.getDestination();

        try (ProtocolHandler destinationProtocolHandler = protocolHandlerResolver.getProtocolHandler(destinationUri);
                ProtocolHandler sourceProtocolHandler = protocolHandlerResolver.getProtocolHandler(sourceUri)) {
            return Throwables.returnableInstance(() -> transfer(sourceUri, destinationUri, destinationProtocolHandler, sourceProtocolHandler));
        }
    }

    private static ContentInfo transfer(URI sourceUri, URI destinationUri, ProtocolHandler destinationProtocolHandler, ProtocolHandler sourceProtocolHandler)
            throws IOException, NoSuchAlgorithmException {
        sourceProtocolHandler.init(sourceUri);
        destinationProtocolHandler.init(destinationUri);

        long contentLength = sourceProtocolHandler.getContentLength();

        try (InputStream inputStream = sourceProtocolHandler.openStream(); HashingInputStream hashingInputStream = new HashingInputStream(Hashing.md5(), inputStream);
                CountingInputStream countingInputStream = new CountingInputStream(hashingInputStream)) {
            destinationProtocolHandler.copyTo(countingInputStream, contentLength);
            String md5 = hashingInputStream.hash().toString();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Transferred " + sourceUri + " to " + destinationUri + ". Calculated md5: " + md5);
            }

            return new ContentInfo(sourceUri, contentLength, ImmutableList.of(new Hash("MD5", md5)));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}
