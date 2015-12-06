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

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.task.providers.manifest.ManifestFile;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerResolver;
import org.jboss.logging.Logger;


import javax.inject.Inject;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Richard Lucas
 */
public class ManifestFileBuilder {

    private static final Logger LOGGER = Logger.getLogger(ManifestFileBuilder.class);

    @Inject
    ProtocolHandlerResolver protocolHandlerResolver;

    URI rootUri;

    public void init(URI manifestUri) {
        rootUri = getRootPathFromManifestUri(manifestUri);
    }

    public ManifestFile build(URI fileUri) {
        if (rootUri == null) {
            throw new OmakaseRuntimeException("ManifestFileBuilder has not been initialized");
        }

        URI absoluteUri = Throwables.returnableInstance(() -> new java.net.URI(rootUri.toString() + "/" + fileUri.toString()));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting file size for " + absoluteUri);
        }

        try (ProtocolHandler protocolHandler = protocolHandlerResolver.getProtocolHandler(absoluteUri)) {
            protocolHandler.init(absoluteUri);
            return new ManifestFile(fileUri, protocolHandler.getContentLength());
        }
    }


    private static URI getRootPathFromManifestUri(URI manifestUri) {
        Path manifestPath = Paths.get(manifestUri.getPath());
        int count = manifestPath.getNameCount();
        String path;
        if (count == 0) {
            path = Paths.get("/").toString();
        } else if (count == 1) {
            path = manifestPath.getRoot().toString();
        } else {
            path = "/" + manifestPath.subpath(0, count - 1);
        }
        return Throwables.returnableInstance(() -> new URI(manifestUri.getScheme(), manifestUri.getUserInfo(), manifestUri.getHost(), manifestUri.getPort(), path, null, null));
    }
}
