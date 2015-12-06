/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker.tool.protocol.provider.file;

import com.google.common.io.Files;
import org.projectomakase.omakase.worker.tool.protocol.HandleProtocol;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * {@link ProtocolHandler} implementation that supports the "file" protocol.
 *
 * @author Richard Lucas
 */
@HandleProtocol("file")
public class FileProtocolHandler implements ProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(FileProtocolHandler.class);

    private URI uri;

    @Override
    public void init(URI uri) {
        validateUriScheme(uri);
        this.uri = uri;
    }

    @Override
    public InputStream openStream() throws IOException {
        isInitiated();
        return new FileInputStream(Paths.get(uri).toFile());
    }

    @Override
    public long getContentLength() {
        isInitiated();
        try {
            return java.nio.file.Files.size(Paths.get(uri));
        } catch (IOException e) {
            throw new ProtocolHandlerException("Failed to get length of " + uri.toString(), e);
        }
    }

    @Override
    public void copyTo(InputStream from, long contentLength) {
        isInitiated();
        try {
            File file = new File(uri);
            Optional.ofNullable(file.getParentFile()).ifPresent(File::mkdirs);
            Files.asByteSink(file).writeFrom(from);
        } catch (IOException e) {
            throw new ProtocolHandlerException("Failed to copy to " + uri.toString(), e);
        }
    }

    @Override
    public void delete() {
        isInitiated();
        try {
            java.nio.file.Files.delete(Paths.get(uri));
        } catch (NoSuchFileException e) {
            LOGGER.info("The file " + uri.toString() + " does not exist. delete() is considered a no-op");
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new ProtocolHandlerException("Failed to delete file " + uri.toString(), e);
        }
    }

    private static void validateUriScheme(URI uri) {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw new ProtocolHandlerException(uri.getScheme() + " is not supported by this protocol handler");
        }
    }

    private void isInitiated() {
        if (uri == null) {
            throw new ProtocolHandlerException("Protocol Handler is not initiated.");
        }
    }
}
