/*
 * #%L
 * omakase-commons
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
package org.projectomakase.omakase.commons.aws.s3;

import com.amazonaws.auth.AWSCredentials;

/**
 * Represents an Upload to S3.
 *
 * @author Richard Lucas
 */
public class S3Upload {

    private final AWSCredentials awsCredentials;
    private final String host;
    private final String region;
    private final String endpoint;
    private final String bucket;
    private final String key;
    private final String uploadId;

    public S3Upload(AWSCredentials awsCredentials, String host, String region, String endpoint, String bucket, String key, String uploadId) {
        this.awsCredentials = awsCredentials;
        this.host = host;
        this.region = region;
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.key = key;
        this.uploadId = uploadId;
    }

    public AWSCredentials getAwsCredentials() {
        return awsCredentials;
    }

    public String getHost() {
        return host;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public String getUploadId() {
        return uploadId;
    }

    @Override
    public String toString() {
        return "S3Upload{" +
                "awsCredentials=" + awsCredentials +
                ", host='" + host + '\'' +
                ", region='" + region + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", bucket='" + bucket + '\'' +
                ", key='" + key + '\'' +
                ", uploadId='" + uploadId + '\'' +
                '}';
    }
}
