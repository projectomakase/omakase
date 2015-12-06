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
package org.projectomakase.omakase.worker.tool.protocol.provider.glacier;

import com.amazonaws.auth.AWSCredentials;
import com.google.common.base.Splitter;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;
import org.projectomakase.omakase.worker.Omakase;
import org.projectomakase.omakase.worker.tool.protocol.HandleProtocol;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Glacier Protocol Handler.
 *
 * @author Richard Lucas
 */
@HandleProtocol("glacier")
public class GlacierProtocolHandler implements ProtocolHandler {

    private URI uri;
    private AWSCredentials awsCredentials;

    @Inject
    @Omakase
    GlacierClient glacierClient;

    @Override
    public void init(URI uri) {
        this.uri = uri;
        this.awsCredentials = AWSClients.credentialsFromUri(uri);
    }

    @Override
    public InputStream openStream() throws IOException {
        String region = AWSClients.glacierHostToRegion(uri.getHost());
        List<String> pathParts = Splitter.on("/").omitEmptyStrings().splitToList(uri.getPath());
        checkArgument(pathParts.size() == 6, "Invalid URI path, " + uri.getPath());
        String vault = pathParts.get(2);
        String jobId = Splitter.on("/").omitEmptyStrings().splitToList(uri.getPath()).get(4);
        return glacierClient.getArchiveRetrievalJobOutput(awsCredentials, region, vault, jobId);
    }

    @Override
    public long getContentLength() {
        throw new UnsupportedOperationException("Glacier does not support direct access to the content's length");
    }

    @Override
    public void copyTo(InputStream from, long contentLength) {
        throw new UnsupportedOperationException("Protocol handler does not support copying to glacier");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

