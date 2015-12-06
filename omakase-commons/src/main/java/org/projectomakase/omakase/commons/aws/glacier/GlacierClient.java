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
package org.projectomakase.omakase.commons.aws.glacier;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.AbortMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
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
import java.util.Optional;

/**
 * Glacier Client implementation.
 * <p>
 * The current implementation currently uses the AWS SDK for some calls and directly invokes the REST API for other calls which is why it requires both a {@link HttpClient} and a {@link
 * AmazonGlacier} client. In the future the dependency on the AWS SDK will be removed which will simplify this class.
 * </p>
 *
 * @author Richard Lucas
 */
public class GlacierClient {

    private static final Logger LOGGER = Logger.getLogger(GlacierClient.class);

    private static final String ARCHIVE_RETRIEVAL = "archive-retrieval";
    private static final String SERVICE = "glacier";

    private final HttpClient httpClient;
    private final AWSRequestSignerV4 awsRequestSignerV4 = new AWSRequestSignerV4();
    private final RuntimeCredentialsProvider runtimeCredentialsProvider;
    private final AmazonGlacier amazonGlacier;

    /**
     * Creates a new Glacier Client configured with it's own pooled Http Client.
     */
    public GlacierClient() {
        this.httpClient = HttpClientFactory.pooledConnectionHttpClient(100, 30000, 600000);
        this.runtimeCredentialsProvider = new RuntimeCredentialsProvider();
        this.amazonGlacier = new AmazonGlacierClient(runtimeCredentialsProvider);
    }

    /**
     * Creates a new AWS Glacier Client.
     *
     * @param httpClient
     *         the underlying http client used by the Glacier client.
     * @param runtimeCredentialsProvider
     *         the {@link RuntimeCredentialsProvider} used when creating the {@link AmazonGlacier} client.
     * @param amazonGlacier
     *         a {@link AmazonGlacier} client instance that has been created using the runtime credential provider above.
     */
    public GlacierClient(HttpClient httpClient, RuntimeCredentialsProvider runtimeCredentialsProvider, AmazonGlacier amazonGlacier) {
        this.httpClient = httpClient;
        this.runtimeCredentialsProvider = runtimeCredentialsProvider;
        this.amazonGlacier = amazonGlacier;
    }


    public String initiateMultipartUpload(AWSCredentials awsCredentials, String region, String vault, String archiveDescription, long partSize) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonGlacier.setRegion(Region.getRegion(Regions.fromName(region)));

            InitiateMultipartUploadResult result = amazonGlacier.initiateMultipartUpload(new InitiateMultipartUploadRequest(vault, archiveDescription, Long.toString(partSize)));
            return result.getUploadId();
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public void uploadMultipartPart(GlacierUpload upload, AWSUploadPart uploadPart, long partSize, InputStream inputStream) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Uploading part " + uploadPart.getNumber() + " to vault " + upload.getVault() + ". Upload Id: " + upload.getUploadId());
        }

        HttpPut put = null;
        try {
            InputStream limitedStream = ByteStreams.limit(inputStream, partSize);
            String host = upload.getHost();
            String endpoint = upload.getEndpoint();
            String uri = "https://" + host + endpoint;
            long rangeFrom = uploadPart.getLength() - 1;
            String range = "bytes " + uploadPart.getOffset() + "-" + rangeFrom + "/*";

            put = new HttpPut(uri);
            put.setHeader("Host", host);
            put.setHeader("x-amz-content-sha256", uploadPart.getSigningHash());
            put.setHeader("x-amz-date", AWSClients.getAWSFormattedDateTime(ZonedDateTime.now()));
            put.setHeader("x-amz-glacier-version", "2012-06-01");
            put.setHeader("Content-Range", range);
            put.setHeader("x-amz-sha256-tree-hash", uploadPart.getPartHash());
            put.setHeader("Authorization", awsRequestSignerV4.createV4Signature(put, SERVICE, upload.getRegion(), endpoint, upload.getAwsCredentials(), uploadPart.getSigningHash()));
            put.setEntity(new NonClosingInputStreamEntity(limitedStream, uploadPart.getLength() - uploadPart.getOffset()));

            HttpResponse httpResponse = httpClient.execute(put);

            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (204 != statusCode) {
                String reason = Optional.of(httpResponse.getEntity()).map(httpEntity -> Throwables.returnableInstance(httpEntity::getContent)).map(is -> Throwables.returnableInstance(() -> {
                    try (InputStreamReader reader = new InputStreamReader(is)) {
                        return CharStreams.toString(reader);
                    }
                })).orElse("none");
                String message = "Failed to upload part to Glacier. HTTP Error Code " + statusCode;
                LOGGER.error(message + " Reason: " + reason);
                throw new OmakaseRuntimeException(message);
            } else {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        } catch (IOException e) {
            put.abort();
            String message = "Failed to upload part to glacier. Reason " + e.getMessage();
            LOGGER.error(message, e);
            throw new OmakaseRuntimeException(message, e);
        }
    }

    public String completeMultipartUpload(AWSCredentials awsCredentials, String region, String vault, String uploadId, long archiveSize, String archiveHash) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonGlacier.setRegion(Region.getRegion(Regions.fromName(region)));

            CompleteMultipartUploadResult result = amazonGlacier.completeMultipartUpload(new CompleteMultipartUploadRequest(vault, uploadId, Long.toString(archiveSize), archiveHash));
            return result.getArchiveId();
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public void abortMultipartUpload(AWSCredentials awsCredentials, String region, String vault, String uploadId) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonGlacier.setRegion(Region.getRegion(Regions.fromName(region)));

            amazonGlacier.abortMultipartUpload(new AbortMultipartUploadRequest(vault, uploadId));
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public void restore(AWSCredentials awsCredentials, String region, String vault, String archiveId, String snsTopic, String correlationId) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonGlacier.setRegion(Region.getRegion(Regions.fromName(region)));

            JobParameters jobParameters = new JobParameters();
            jobParameters.setType(ARCHIVE_RETRIEVAL);
            jobParameters.setArchiveId(archiveId);
            jobParameters.setSNSTopic(snsTopic);
            jobParameters.setDescription(correlationId);
            InitiateJobResult result = amazonGlacier.initiateJob(new InitiateJobRequest(vault, jobParameters));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created restore job " + result.getJobId() + " for archive " + archiveId + " in region/vault" + region + "/" + vault);
            }
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public InputStream getArchiveRetrievalJobOutput(AWSCredentials credentials, String region, String vault, String jobId) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(credentials);
            amazonGlacier.setRegion(Region.getRegion(Regions.fromName(region)));

            GetJobOutputRequest request = new GetJobOutputRequest();
            request.setVaultName(vault);
            request.setJobId(jobId);
            GetJobOutputResult result = amazonGlacier.getJobOutput(request);
            return result.getBody();
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    public void deleteArchive(AWSCredentials awsCredentials, String region, String vault, String archiveId) {
        try {
            runtimeCredentialsProvider.setAwsCredentials(awsCredentials);
            amazonGlacier.setRegion(Region.getRegion(Regions.fromName(region)));
            amazonGlacier.deleteArchive(new DeleteArchiveRequest(vault, archiveId));
        } catch (AmazonClientException e) {
            throw new OmakaseRuntimeException(e);
        }
    }
}
