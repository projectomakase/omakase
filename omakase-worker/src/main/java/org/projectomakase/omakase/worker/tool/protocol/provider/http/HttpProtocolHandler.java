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
package org.projectomakase.omakase.worker.tool.protocol.provider.http;

import org.projectomakase.omakase.worker.Omakase;
import org.projectomakase.omakase.worker.tool.protocol.HandleProtocol;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * {@link ProtocolHandler} implementation that supports the "http" protocol.
 * <p>
 * Does not support writing to or deleting a HTTP uri.
 * </p>
 *
 * @author Richard Lucas
 */
@HandleProtocol("http")
public class HttpProtocolHandler implements ProtocolHandler {

    @Inject
    @Omakase
    HttpClient httpClient;

    private URI uri;

    @Override
    public void init(URI uri) {
        validateUriScheme(uri);
        this.uri = uri;
    }

    @Override
    public InputStream openStream() throws IOException {
        isInitiated();
        HttpGet request = new HttpGet(uri);
        HttpResponse response = httpClient.execute(request);
        if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
            throw new ProtocolHandlerException("An error occurred connecting to uri " + uri);
        }
        return response.getEntity().getContent();
    }

    @Override
    public long getContentLength() {
        isInitiated();
        try {
            HttpHead request = new HttpHead(uri);
            HttpResponse response = httpClient.execute(request);
            if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                throw new ProtocolHandlerException("An error occurred connecting to uri " + uri);
            }
            return Long.valueOf(response.getLastHeader(HttpHeaders.CONTENT_LENGTH).getValue());
        } catch (IOException e) {
            throw new ProtocolHandlerException("Unable to retrieve content length for URI " + uri, e);
        }
    }

    @Override
    public void copyTo(InputStream from, long contentLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    private void isInitiated() {
        if (uri == null) {
            throw new ProtocolHandlerException("Protocol Handler is not initiated.");
        }
    }

    private static void validateUriScheme(URI uri) {
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new ProtocolHandlerException(uri.getScheme() + " is not supported by this protocol handler");
        }
    }
}
