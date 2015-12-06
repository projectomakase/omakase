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

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.AWSRequestSignerV4;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.aws.RuntimeCredentialsProvider;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.http.HttpClientFactory;
import org.projectomakase.omakase.commons.http.NonClosingInputStreamEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * S3 Client implementation.
 * <p>
 * The current implementation currently uses the AWS SDK for some calls and directly invokes the REST API for other calls which is why it requires both a {@link HttpClient} and a {@link
 * AmazonS3} client. In the future the dependency on the AWS SDK will be removed which will simplify this class.
 * </p>
 *
 * @author Richard Lucas
 */
public class S3Client {

    private static final Logger LOGGER = Logger.getLogger(S3Client.class);

    private static final String SERVICE = "s3";

    private final HttpClient httpClient;
    private final AWSRequestSignerV4 awsRequestSignerV4 = new AWSRequestSignerV4();
    private final RuntimeCredentialsProvider runtimeCredentialsProvider;
    private final AmazonS3 amazonS3;

    public S3Client() {
        this.httpClient = HttpClientFactory.pooledConnectionHttpClient(100, 30000, 600000);
        this.runtimeCredentialsProvider = new RuntimeCredentialsProvider();
        this.amazonS3 = new AmazonS3Client(runtimeCredentialsProvider, new ClientConfiguration().withSignerOverride("AWSS3V4SignerType"));
    }

    public S3Client(HttpClient httpClient, RuntimeCredentialsProvider runtimeCredentialsProvider, AmazonS3 amazonS3) {
        this.httpClient = httpClient;
        this.runtimeCredentialsProvider = runtimeCredentialsProvider;
        this.amazonS3 = amazonS3;
    }

    public void uploadObject(S3Upload upload, String originalFileName, String signingHash, String contentHash, long contentLength, InputStream inputStream) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Uploading to object " + upload.getKey() + " in bucket " + upload.getBucket());
        }

        HttpPut put = null;
        try {
            String host = upload.getHost();
            String endpoint = upload.getEndpoint();
            String uri = "https://" + host + endpoint;

            put = new HttpPut(uri);
            put.setHeader("Host", host);
            put.setHeader("x-amz-content-sha256", signingHash);
            put.setHeader("Content-MD5", contentHash);
            put.setHeader("x-amz-date", AWSClients.getAWSFormattedDateTime(ZonedDateTime.now()));
            if (originalFileName != null) {
                put.setHeader("x-amz-meta-original-filename", originalFileName);
            }
            put.setHeader("Authorization", awsRequestSignerV4.createV4Signature(put, SERVICE, upload.getRegion(), endpoint, upload.getAwsCredentials(), signingHash));
            put.setEntity(new InputStreamEntity(inputStream, contentLength));

            HttpResponse httpResponse = httpClient.execute(put);

            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (200 != statusCode) {
                String reason = Optional.of(httpResponse.getEntity()).map(httpEntity -> Throwables.returnableInstance(httpEntity::getContent)).map(is -> Throwables.returnableInstance(() -> {
                    try (InputStreamReader reader = new InputStreamReader(is)) {
                        return CharStreams.toString(reader);
                    }
                })).orElse("none");
                String message = "Failed to upload object to S3. HTTP Error Code " + statusCode + " Reason: " + reason;
                LOGGER.error(message);
                throw new OmakaseRuntimeException(message);
            } else {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        } catch (IOException e) {
            put.abort();
            String message = "Failed to upload object to S3. Reason " + e.getMessage();
            LOGGER.error(message, e);
            throw new OmakaseRuntimeException(message, e);
        }
    }

    public String initiateMultipartUpload(S3Upload upload, String originalFilename) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(upload.getAwsCredentials());
            amazonS3.setRegion(Region.getRegion(Regions.fromName(upload.getRegion())));
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.addUserMetadata("original-filename", originalFilename);
            InitiateMultipartUploadResult result = amazonS3.initiateMultipartUpload(new InitiateMultipartUploadRequest(upload.getBucket(), upload.getKey(), objectMetadata));
            return result.getUploadId();
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public S3Part uploadMultipartPart(S3Upload upload, AWSUploadPart uploadPart, long partSize, InputStream inputStream) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Uploading part " + uploadPart.getNumber() + " to object " + upload.getKey() + " in bucket " + upload.getBucket() + " Upload Id: " + upload.getUploadId());
        }

        HttpPut put = null;
        try {
            InputStream limitedStream = ByteStreams.limit(inputStream, partSize);
            String host = upload.getHost();
            int part = uploadPart.getNumber() + 1;
            String endpoint = upload.getEndpoint();
            String uri = "https://" + host + endpoint + "?partNumber=" + part + "&uploadId=" + upload.getUploadId();

            put = new HttpPut(uri);
            put.setHeader("Host", host);
            put.setHeader("x-amz-content-sha256", uploadPart.getSigningHash());
            put.setHeader("Content-MD5", uploadPart.getPartHash());
            put.setHeader("x-amz-date", AWSClients.getAWSFormattedDateTime(ZonedDateTime.now()));
            put.setHeader("Authorization", awsRequestSignerV4.createV4Signature(put, SERVICE, upload.getRegion(), endpoint, upload.getAwsCredentials(), uploadPart.getSigningHash()));
            put.setEntity(new NonClosingInputStreamEntity(limitedStream, uploadPart.getLength() - uploadPart.getOffset()));

            HttpResponse httpResponse = httpClient.execute(put);

            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (200 != statusCode) {
                String reason = Optional.of(httpResponse.getEntity()).map(httpEntity -> Throwables.returnableInstance(httpEntity::getContent)).map(is -> Throwables.returnableInstance(() -> {
                    try (InputStreamReader reader = new InputStreamReader(is)) {
                        return CharStreams.toString(reader);
                    }
                })).orElse("none");
                String message = "Failed to upload part to S3. HTTP Error Code " + statusCode + " Reason: " + reason;
                LOGGER.error(message);
                throw new OmakaseRuntimeException(message);
            } else {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
                return new S3Part(part, httpResponse.getFirstHeader("Etag").getValue().replace("\"", ""));
            }
        } catch (IOException e) {
            put.abort();
            String message = "Failed to upload part to S3. Reason " + e.getMessage();
            LOGGER.error(message, e);
            throw new OmakaseRuntimeException(message, e);
        }
    }

    public void completeMultipartUpload(S3Upload upload, List<S3Part> parts) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(upload.getAwsCredentials());
            amazonS3.setRegion(Region.getRegion(Regions.fromName(upload.getRegion())));
            amazonS3.completeMultipartUpload(new CompleteMultipartUploadRequest(upload.getBucket(), upload.getKey(), upload.getUploadId(),
                                                                                parts.stream().map(s3Part -> new PartETag(s3Part.getNumber(), s3Part.getEtag())).collect(Collectors.toList())));
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public void abortMultipartUpload(S3Upload upload) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(upload.getAwsCredentials());
            amazonS3.setRegion(Region.getRegion(Regions.fromName(upload.getRegion())));
            amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(upload.getBucket(), upload.getKey(), upload.getUploadId()));
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public InputStream getObject(AWSCredentials awsCredentials, String region, String bucket, String key) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting object [region: " + region + ", bucket: " + bucket + ", key: " + key);
        }
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonS3.setRegion(Region.getRegion(Regions.fromName(region)));
            return amazonS3.getObject(bucket, key).getObjectContent();
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public long getObjectLength(AWSCredentials awsCredentials, String region, String bucket, String key) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonS3.setRegion(Region.getRegion(Regions.fromName(region)));
            return amazonS3.getObjectMetadata(bucket, key).getContentLength();
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public void deleteObject(AWSCredentials awsCredentials, String region, String bucket, String key) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonS3.setRegion(Region.getRegion(Regions.fromName(region)));
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, key));
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }
}
