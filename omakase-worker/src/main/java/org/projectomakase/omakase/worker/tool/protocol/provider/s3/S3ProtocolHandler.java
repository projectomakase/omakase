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
package org.projectomakase.omakase.worker.tool.protocol.provider.s3;

import com.amazonaws.auth.AWSCredentials;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.s3.S3Client;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;
import org.projectomakase.omakase.commons.hash.ByteRange;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.hash.HashByteProcessor;
import org.projectomakase.omakase.commons.hash.HashStrategy;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.worker.Omakase;
import org.projectomakase.omakase.worker.tool.protocol.HandleProtocol;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * S3 Protocol Handler.
 *
 * @author Richard Lucas
 */
@HandleProtocol("s3")
public class S3ProtocolHandler implements ProtocolHandler {

    private URI uri;
    private AWSCredentials awsCredentials;
    private String region;
    private String bucket;
    private String key;
    private Optional<String> originalFileName;

    @Inject
    @Omakase
    S3Client s3Client;
    @Inject
    @ConfigProperty(name = "s3.buffer.size.bytes", defaultValue = "5242880")
    int bufferSize;

    @Override
    public void init(URI uri) {
        this.uri = uri;
        this.awsCredentials = AWSClients.credentialsFromUri(uri);
        this.region = AWSClients.s3HostToRegion(uri.getHost());
        this.bucket = uri.getHost().substring(0, uri.getHost().indexOf("."));
        this.key = uri.getPath().substring(1);
        this.originalFileName = Optional.ofNullable(uri.getQuery()).map(query -> Splitter.on("&").withKeyValueSeparator("=").split(uri.getQuery()).get("originalFileName"));
    }

    @Override
    public InputStream openStream() throws IOException {
        return s3Client.getObject(awsCredentials, region, bucket, key);
    }

    @Override
    public long getContentLength() {
        return s3Client.getObjectLength(awsCredentials, region, bucket, key);
    }

    @Override
    public void copyTo(InputStream from, long contentLength) {
        if (contentLength > bufferSize) {
            throw new ProtocolHandlerException("The content length " + contentLength + " is greater than the configured buffer size " + bufferSize);
        }
        byte[] inMemoryBuffer = copyToBuffer(from);

        List<Hash> hashes = generateHashes(contentLength, inMemoryBuffer);
        Hash sha256 = getHash(hashes, Hashes.SHA256);
        Hash md5Base64 = getHash(hashes, Hashes.MD5_BASE64);

        S3Upload s3Upload = AWSClients.s3UploadFromURI(uri);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inMemoryBuffer)) {
            s3Client.uploadObject(s3Upload, originalFileName.orElse(null), sha256.getValue(), md5Base64.getValue(), contentLength, byteArrayInputStream);
        } catch (IOException e) {
            throw new ProtocolHandlerException("Failed to copy to S3", e);
        }
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not implemented");
    }

    private static Hash getHash(List<Hash> hashes, String algorithm) {
        return hashes.stream().filter(hash -> hash.getAlgorithm().equals(algorithm)).findFirst().orElseThrow(() -> new ProtocolHandlerException("Failed to generate" + algorithm + " hash"));
    }


    private static byte[] copyToBuffer(InputStream from) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ByteStreams.copy(from, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ProtocolHandlerException("Failed to copy to buffer", e);
        }
    }

    private static HashStrategy getHashStrategy(String algorithm, long contentLength) {
        return Hashes.getHashStrategy(algorithm, new ByteRange(0, contentLength - 1));
    }

    private static List<Hash> generateHashes(long contentLength, byte[] inMemoryBuffer) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inMemoryBuffer)) {
            List<HashStrategy> hashStrategies = ImmutableList.of(getHashStrategy(Hashes.SHA256, contentLength), getHashStrategy(Hashes.MD5_BASE64, contentLength));
            return ByteStreams.readBytes(byteArrayInputStream, new HashByteProcessor(hashStrategies));
        } catch (IOException e) {
            throw new ProtocolHandlerException("Failed to generate hash values", e);
        }
    }
}

