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
import org.projectomakase.omakase.commons.aws.s3.S3Client;
import org.projectomakase.omakase.commons.aws.s3.S3Part;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;

import java.io.InputStream;
import java.util.List;

/**
 * @author Richard Lucas
 */
public class MockS3Client extends S3Client {

    private final IdGenerator idGenerator = new IdGenerator();

    public MockS3Client() {
        super(null, null, null);
    }

    @Override
    public void uploadObject(S3Upload upload, String originalFileName, String signingHash, String contentHash, long contentLength, InputStream inputStream) {
        // no-op
    }

    @Override
    public String initiateMultipartUpload(S3Upload upload, String originalFilename) {
        return idGenerator.getId();
    }

    @Override
    public S3Part uploadMultipartPart(S3Upload upload, AWSUploadPart uploadPart, long partSize, InputStream inputStream) {
        return new S3Part(uploadPart.getNumber() + 1, idGenerator.getId());
    }

    @Override
    public void completeMultipartUpload(S3Upload upload, List<S3Part> parts) {
        // no-op
    }

    @Override
    public void abortMultipartUpload(S3Upload upload) {
        // no-op
    }

    @Override
    public void deleteObject(AWSCredentials awsCredentials, String region, String bucket, String key) {
        // no-op
    }
}
