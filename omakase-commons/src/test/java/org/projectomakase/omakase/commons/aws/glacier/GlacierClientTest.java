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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.aws.AWSClients;
import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.commons.file.FileGenerator;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

/**
 * @author Richard Lucas
 */
public class GlacierClientTest {

    private static final Logger LOGGER = Logger.getLogger(GlacierClientTest.class);

    private GlacierClient glacierClient;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        glacierClient = new GlacierClient();
    }

    // Disabled by default, used to validate end to end multipart upload to Glacier is working
    @Test
    @Ignore
    public void shouldUploadToGlacierVault() throws Exception {
        File testFile = FileGenerator.generate(temporaryFolder.getRoot(), (1024 * 1024 * 10));

        // create SHA-256 and MD5 hashes for each part



        EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
        AWSCredentials credentials = credentialsProvider.getCredentials();

        String uploadId = glacierClient.initiateMultipartUpload(credentials, "us-west-1", "dev", "testing", 4194304);
        try (FileInputStream fileInputStream = new FileInputStream(testFile)) {

            URI destination =
                    new URI("glacier://" + credentials.getAWSAccessKeyId() + ":" + credentials.getAWSSecretKey() + "@glacier.us-west-1.amazonaws.com/-/vaults/dev/multipart-uploads/" + uploadId);

            GlacierUpload glacierUpload = AWSClients.glacierUploadFromURI(destination);

            ImmutableList.Builder<AWSUploadPart> uploadPartBuilder = ImmutableList.builder();
            uploadPartBuilder
                    .add(new AWSUploadPart(0, 0, 4194304, "c72bed74619a0238b707f13fd6ee8bec14cc7779caca89248350ebc1ac9725ec", "ec9364f600b5e47c533056a548fda46affb964198244fe9d08540ad3884aa89f"));
            uploadPartBuilder.add(new AWSUploadPart(1, 4194304, 8388608, "dfc5f7fead60530c8f6155ef3405e7b1f21bdabf2dc2a7dc26573ad6dd44251e",
                    "9deb0b59576f96e20efb79d09c274b6db16533314d37d1f1b74145283550031f"));
            uploadPartBuilder.add(new AWSUploadPart(2, 8388608, 10486525, "1da69b95fc1a32b26957c70b39fd3adf88c4726f77cf338fbeecf43bf50c0067",
                    "1e7d14612ffb9cb4dae4dcedb8fd24dbd4d916fbf93677e782074dfc5986f54c"));

            uploadPartBuilder.build().forEach(uploadPart -> glacierClient.uploadMultipartPart(glacierUpload, uploadPart, 4194304, fileInputStream));
            String archiveId = glacierClient.completeMultipartUpload(credentials, "us-west-1", "dev", uploadId, testFile.length(), "2b30a705e4dcfd60c89a5596c6d4d1ddf5c1a1576271fbada358a319175020cd");
        } catch (Exception e) {
            LOGGER.error(e);
            glacierClient.abortMultipartUpload(credentials, "us-west-1", "dev", uploadId);
        }
    }
}