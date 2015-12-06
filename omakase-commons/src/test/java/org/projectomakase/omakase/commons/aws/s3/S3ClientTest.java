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
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.file.FileGenerator;
import org.projectomakase.omakase.commons.hash.ByteRange;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.commons.hash.HashByteProcessor;
import org.projectomakase.omakase.commons.hash.HashStrategy;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import java.io.File;
import java.io.FileInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Richard Lucas
 */
public class S3ClientTest {

    private static final Logger LOGGER = Logger.getLogger(S3ClientTest.class);
    private static final String BUCKET = "oma-dev";
    private static final String KEY = "test2.dat";
    private static final String HOST = BUCKET + ".s3-us-west-1.amazonaws.com";
    private static final int PART_SIZE = 1024 * 1024 * 5;

    private S3Client s3Client;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        s3Client = new S3Client();
    }

    // Disabled by default, used to validate end to end upload to S3 is working
    @Test
    @Ignore
    public void shouldPerformUploadToS3Bucket() throws Exception {
        File testFile = FileGenerator.generate(temporaryFolder.getRoot(), (1024 * 1024 * 1));
        ByteRange byteRange = new ByteRange(0, testFile.length() - 1);
        ImmutableList<HashStrategy> hashStrategies = ImmutableList.of(Hashes.getHashStrategy(Hashes.SHA256, byteRange), Hashes.getHashStrategy(Hashes.MD5_BASE64, byteRange));

        List<Hash> hashes;
        try (FileInputStream fileInputStream = new FileInputStream(testFile)) {
            hashes = ByteStreams.readBytes(fileInputStream, new HashByteProcessor(hashStrategies));
        }
        Comparator<Hash> hashComparator = (o1, o2) -> o1.getOffset().compareTo(o2.getOffset());
        List<Hash> signingHashes = hashes.stream().filter(hash -> hash.getAlgorithm().equals(Hashes.SHA256)).sorted(hashComparator).collect(ImmutableListCollector.toImmutableList());
        List<Hash> partHashes = hashes.stream().filter(hash -> hash.getAlgorithm().equals(Hashes.MD5_BASE64)).sorted(hashComparator).collect(ImmutableListCollector.toImmutableList());


        EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
        AWSCredentials credentials = credentialsProvider.getCredentials();

        S3Upload s3Upload = new S3Upload(credentials, HOST, "us-west-1", "/" + KEY, BUCKET, KEY, null);

        try (FileInputStream fileInputStream = new FileInputStream(testFile)) {
            s3Client.uploadObject(s3Upload, "testing", signingHashes.get(0).getValue(), partHashes.get(0).getValue(), testFile.length(), fileInputStream);
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

    // Disabled by default, used to validate end to end multipart upload to S3 is working
    @Test
    @Ignore
    public void shouldPerformMultipartUploadToS3Bucket() throws Exception {
        File testFile = FileGenerator.generate(temporaryFolder.getRoot(), (1024 * 1024 * 10));

        ImmutableList<HashStrategy> hashStrategies = Hashes.createByteRanges(PART_SIZE, testFile.length()).stream()
                .map(byteRange -> ImmutableList.of(Hashes.getHashStrategy(Hashes.SHA256, byteRange), Hashes.getHashStrategy(Hashes.MD5_BASE64, byteRange))).collect(ImmutableListsCollector.toImmutableList());

        List<Hash> hashes;
        try (FileInputStream fileInputStream = new FileInputStream(testFile)) {
            hashes = ByteStreams.readBytes(fileInputStream, new HashByteProcessor(hashStrategies));
        }
        Comparator<Hash> hashComparator = (o1, o2) -> o1.getOffset().compareTo(o2.getOffset());
        List<Hash> signingHashes = hashes.stream().filter(hash -> hash.getAlgorithm().equals(Hashes.SHA256)).sorted(hashComparator).collect(ImmutableListCollector.toImmutableList());
        List<Hash> partHashes = hashes.stream().filter(hash -> hash.getAlgorithm().equals(Hashes.MD5_BASE64)).sorted(hashComparator).collect(ImmutableListCollector.toImmutableList());
        List<AWSUploadPart> uploadParts = IntStream.range(0, signingHashes.size()).mapToObj(index -> {
            Hash signingHash = signingHashes.get(index);
            Hash partHash = partHashes.get(index);
            return new AWSUploadPart(index, signingHash.getOffset(), signingHash.getLength().get(), signingHash.getValue(), partHash.getValue());
        }).collect(ImmutableListCollector.toImmutableList());


        EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
        AWSCredentials credentials = credentialsProvider.getCredentials();

        S3Upload initS3Upload = new S3Upload(credentials, HOST, "us-west-1", "/" + KEY, BUCKET, KEY, null);
        String uploadId = s3Client.initiateMultipartUpload(initS3Upload, testFile.getName());

        S3Upload s3Upload = new S3Upload(credentials, HOST, "us-west-1", "/" + KEY, BUCKET, KEY, uploadId);

        try (FileInputStream fileInputStream = new FileInputStream(testFile)) {
            List<S3Part> s3Parts = uploadParts.stream().map(uploadPart -> s3Client.uploadMultipartPart(s3Upload, uploadPart, PART_SIZE, fileInputStream)).collect(Collectors.toList());
            s3Client.completeMultipartUpload(s3Upload, s3Parts);
        } catch (Exception e) {
            LOGGER.error(e, e);
            s3Client.abortMultipartUpload(s3Upload);
        }
    }

}