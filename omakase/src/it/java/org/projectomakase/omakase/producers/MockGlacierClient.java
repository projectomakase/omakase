/*
 * #%L
 * omakase
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
package org.projectomakase.omakase.producers;

import com.amazonaws.auth.AWSCredentials;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;
import org.projectomakase.omakase.commons.aws.glacier.GlacierUpload;

import java.io.InputStream;

/**
 * @author Richard Lucas
 */
public class MockGlacierClient extends GlacierClient {

    private final IdGenerator idGenerator = new IdGenerator();

    public MockGlacierClient() {
        super(null, null, null);
    }

    @Override
    public String initiateMultipartUpload(AWSCredentials awsCredentials, String region, String vault, String archiveDescription, long partSize) {
        return idGenerator.getId();
    }

    @Override
    public void uploadMultipartPart(GlacierUpload upload, AWSUploadPart uploadPart, long partSize, InputStream inputStream) {
        // no-op
    }

    @Override
    public String completeMultipartUpload(AWSCredentials awsCredentials, String region, String vault, String uploadId, long archiveSize, String archiveHash) {
        return "test-archive-id";
    }

    @Override
    public void abortMultipartUpload(AWSCredentials awsCredentials, String region, String vault, String uploadId) {
        // no-op
    }

    @Override
    public void restore(AWSCredentials awsCredentials, String region, String vault, String archiveId, String snsTopic, String correlationId) {
        // no-op
    }

    @Override
    public InputStream getArchiveRetrievalJobOutput(AWSCredentials credentials, String region, String vault, String jobId) {
        return null;
    }
}
