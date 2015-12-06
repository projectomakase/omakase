/*
 * #%L
 * omakase-tool-manifest
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
package org.projectomakase.omakase.worker.tool.manifest;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskOutput;
import org.projectomakase.omakase.worker.tool.Tool;
import org.projectomakase.omakase.worker.tool.ToolCallback;
import org.projectomakase.omakase.worker.tool.ToolException;
import org.projectomakase.omakase.worker.tool.manifest.dash.DashManifestParser;
import org.projectomakase.omakase.worker.tool.manifest.hls.HLSManifestParser;
import org.projectomakase.omakase.worker.tool.manifest.smooth.SmoothManifestParser;
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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link Tool} implementation used to parse and transfer manifest files.
 *
 * @author Richard Lucas
 */
@Named(ManifestTool.NAME)
public class ManifestTool implements Tool {

    public static final String NAME = "MANIFEST_TRANSFER";

    private static final Logger LOGGER = Logger.getLogger(ManifestTool.class);
    private static final String HLS = ".m3u8";
    private static final String DASH = ".mpd";
    private static final String SMOOTH = ".ism";

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;
    @Inject
    Event<ToolCallback> event;
    @Inject
    DashManifestParser dashManifestParser;
    @Inject
    HLSManifestParser hlsManifestParser;
    @Inject
    SmoothManifestParser smoothManifestParser;

    @Override
    public String getName() {
        return NAME;
    }

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
            ManifestTransferTaskConfiguration configuration = (ManifestTransferTaskConfiguration) task.getConfiguration();
            checkArgument(configuration.getSource() != null, "source uri can not be null");
            checkArgument(configuration.getDestination() != null, "destination uri can not be null");

            URI sourceUri = configuration.getSource();
            URI destinationUri = configuration.getDestination();

            try (ProtocolHandler destinationProtocolHandler = protocolHandlerResolver.getProtocolHandler(destinationUri);
                    ProtocolHandler sourceProtocolHandler = protocolHandlerResolver.getProtocolHandler(sourceUri)) {

                sourceProtocolHandler.init(sourceUri);
                destinationProtocolHandler.init(destinationUri);

                long contentLength = sourceProtocolHandler.getContentLength();
                ManifestParserResult result = parse(sourceUri, sourceProtocolHandler);
                String md5 = copy(sourceProtocolHandler, destinationProtocolHandler, contentLength);
                ManifestTransferTaskOutput taskOutput = getManifestTransferTaskOutput(result, contentLength, md5);

                String message = "Parsed and transferred manifest " + sourceUri + " to " + destinationUri + ". Calculated md5: " + md5;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(message);
                }

                event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.COMPLETED, message, 100, taskOutput)));

            }

        } catch (Exception e) {
            LOGGER.error("IOTool failed. Reason: " + e.getMessage(), e);
            event.fire(new ToolCallback(NAME, taskId, new TaskStatusUpdate(TaskStatus.FAILED_DIRTY, "Failed to parse and transfer manifest. Reason: " + e.getMessage(), 0)));
        }
    }

    private ManifestParserResult parse(URI sourceUri, ProtocolHandler sourceProtocolHandler) throws IOException {
        try (InputStream manifestInputStream = sourceProtocolHandler.openStream()) {
            return getManifestParser(sourceUri).parse(sourceUri, manifestInputStream);
        }
    }

    private static String copy(ProtocolHandler sourceProtocolHandler, ProtocolHandler destinationProtocolHandler, long contentLength)
            throws IOException, NoSuchAlgorithmException {

        try (InputStream inputStream = sourceProtocolHandler.openStream(); HashingInputStream hashingInputStream = new HashingInputStream(Hashing.md5(), inputStream);
                CountingInputStream countingInputStream = new CountingInputStream(hashingInputStream)) {
            destinationProtocolHandler.copyTo(countingInputStream, contentLength);
            return hashingInputStream.hash().toString();
        }
    }

    private static ManifestTransferTaskOutput getManifestTransferTaskOutput(ManifestParserResult result, long length, String md5) {
        return new ManifestTransferTaskOutput(result.getManifests(), result.getFiles(), length, ImmutableList.of(new Hash("MD5", md5)));
    }

    private ManifestParser getManifestParser(URI sourceUri) {
        String path = sourceUri.getPath().toLowerCase();
        if (path.endsWith(HLS)) {
            return hlsManifestParser;
        } else if (path.endsWith(DASH)) {
            return dashManifestParser;
        } else if (path.endsWith(SMOOTH)) {
            return smoothManifestParser;
        } else {
            throw new ToolException("Unsupported manifest type " + path);
        }
    }
}
