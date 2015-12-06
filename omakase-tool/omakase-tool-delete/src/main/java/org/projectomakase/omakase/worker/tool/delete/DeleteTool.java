/*
 * #%L
 * omakase-tool-delete
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
package org.projectomakase.omakase.worker.tool.delete;

import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.streams.Streams;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.delete.DeleteTaskConfiguration;
import org.projectomakase.omakase.worker.tool.Tool;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerResolver;
import org.jboss.logging.Logger;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Delete Tool.
 * <p>
 * Responsible for executing delete tasks.
 * </p>
 *
 * @author Richard Lucas
 */
@Named(DeleteTool.NAME)
public class DeleteTool implements Tool {

    public static final String NAME = "DELETE";

    private static final Logger LOGGER = Logger.getLogger(DeleteTool.class);

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;

    @Inject
    Event<ToolCallback> event;

    @Override
    public void execute(Task task) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received task " + task.toString());
        }

        String taskId = task.getId();

        try {
            DeleteTaskConfiguration configuration = (DeleteTaskConfiguration) task.getConfiguration();
            checkArgument(configuration.getLocations() != null, "locations can not be null");
            checkArgument(!configuration.getLocations().isEmpty(), "locations can not be empty");
            Set<String> failed = configuration.getLocations().parallelStream().map(this::delete).flatMap(Streams::optionalToStream).collect(ImmutableSetCollector.toImmutableSet());
            if (failed.isEmpty()) {
                event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.COMPLETED, "Deleted files", 100)));
            } else {
                event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed to delete files " + failed, 0)));
            }
        } catch (Exception e) {
            LOGGER.error("Delete Tool failed. Reason: " + e.getMessage(), e);
            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed to delete files. Reason: " + e.getMessage(), 0)));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private Optional<String> delete(URI sourceUri) {
        try (ProtocolHandler sourceProtocolHandler = protocolHandlerResolver.getProtocolHandler(sourceUri)) {
            sourceProtocolHandler.init(sourceUri);
            sourceProtocolHandler.delete();
            String message = "Deleted " + sourceUri.toString();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message);
            }
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Failed to delete file " + sourceUri, e);
            return Optional.of("Failed to delete file " + sourceUri + ". Reason: " + e.getMessage());
        }
    }
}
