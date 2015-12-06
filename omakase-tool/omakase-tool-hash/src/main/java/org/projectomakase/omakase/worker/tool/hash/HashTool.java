/*
 * #%L
 * omakase-tool-hash
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
package org.projectomakase.omakase.worker.tool.hash;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.projectomakase.omakase.commons.hash.ByteRange;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.hash.HashByteProcessor;
import org.projectomakase.omakase.commons.hash.HashStrategy;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.hash.HashInput;
import org.projectomakase.omakase.task.providers.hash.HashTaskConfiguration;
import org.projectomakase.omakase.task.providers.hash.HashTaskOutput;
import org.projectomakase.omakase.worker.tool.Tool;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerResolver;
import org.jboss.logging.Logger;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;
import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;

/**
 * Hash Tool.
 * <p>
 * Responsible for executing hashing tasks.
 * </p>
 *
 * @author Richard Lucas
 */
@Named(HashTool.NAME)
public class HashTool implements Tool {

    private static final Logger LOGGER = Logger.getLogger(HashTool.class);

    public static final String NAME = "HASH";

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;
    @Inject
    Event<ToolCallback> event;

    @Override
    public void execute(Task task) {
        String taskId = task.getId();
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing task " + task.toString());
            }

            HashTaskConfiguration configuration = (HashTaskConfiguration) task.getConfiguration();
            checkArgument(configuration != null, "the task configuration can not be null");
            checkArgument(configuration.getSource() != null, "the  task configuration does not contain a 'source' property");
            checkArgument(configuration.getHashes() != null, "the task configuration does not contain a 'hashes' property");
            checkArgument(!configuration.getHashes().isEmpty(), "the task configuration should have one or more 'hashes'");

            URI sourceUri = configuration.getSource();
            try (ProtocolHandler protocolHandler = protocolHandlerResolver.getProtocolHandler(sourceUri)) {
                protocolHandler.init(sourceUri);
                try (InputStream inputStream = protocolHandler.openStream()) {
                    long contentLength = protocolHandler.getContentLength();
                    ImmutableList<HashStrategy> hashStrategies = configuration.getHashes().stream().map(hashInput -> getHashStrategy(hashInput, contentLength)).collect(toImmutableList());
                    ImmutableList<Hash> hashes = ByteStreams.readBytes(inputStream, new HashByteProcessor(hashStrategies));
                    event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.COMPLETED, "Created requested hash values", 100, new HashTaskOutput(hashes))));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute task. Reason: " + e.getMessage(), e);
            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "Failed to execute hash tool. Reason: " + e.getMessage(), 0)));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private static HashStrategy getHashStrategy(HashInput hashInput, long contentSize) {
        long from = hashInput.getOffset().orElse(0L);
        long to = hashInput.getLength().orElse(contentSize) - 1;
        return Hashes.getHashStrategy(hashInput.getAlgorithm(), new ByteRange(from, to));
    }
}
