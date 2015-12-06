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
package org.projectomakase.omakase.worker.tool.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Service provider for executing IO operations on different URI protocols (schemes).
 * <p>
 * Implementations are located and instantiated via CDI and must be annotated with the qualifier {@link HandleProtocol}
 * </p>
 * <p>
 * A protocol handler needs to be initiated prior to use and closed in order to clean up resources. {@link ProtocolHandler} extends {@link
 * AutoCloseable} in order to allow try-resource blocks. The protocol handler should <b>NOT</b> be reused.
 * </p>
 * <p>
 * Expected usage:
 * </p>
 * <pre>
 * {@code
 *
 * ProtocolHandler protocolHandler = new ...
 * protocolHandler.init(uri);
 * ...
 * protocolHandler.close();
 * }
 * </pre>
 *
 * @author Richard Lucas
 */
public interface ProtocolHandler extends AutoCloseable {

    /**
     * Initializes the protocol handler.
     *
     * @param uri
     *         the uri the protocol handler will use when executing operations.
     */
    void init(URI uri);

    /**
     * Opens a new {@link java.io.InputStream} for reading the protocol handler URI. This method should return a new,
     * independent stream each time it is called. The caller is responsible for cleaning up the input stream.
     *
     * @return an input stream that can be used to read the protocol handlers URI
     * @throws java.io.IOException
     *         if an I/O error occurs in the process of opening the stream
     */
    InputStream openStream() throws IOException;

    /**
     * Returns the length of the content identified by the protocol handler URI.
     *
     * @return the length of the content identified by the protocol handler URI.
     */
    long getContentLength();

    /**
     * Copies all bytes from the specified {@link java.io.InputStream} to the protocol handler URI.
     *
     * @param from
     *         the input stream
     * @param contentLength
     *         the length of the content being copied
     */
     void copyTo(InputStream from, long contentLength);

    /**
     * Deletes the content at the specified URI.
     */
    void delete();

    /**
     * Default close implementation. Performs a no-op.
     */
    @Override
    default void close() {
        //no-op
    }
}
