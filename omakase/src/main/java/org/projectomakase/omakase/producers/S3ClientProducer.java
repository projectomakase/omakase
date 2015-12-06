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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.aws.RuntimeCredentialsProvider;
import org.projectomakase.omakase.commons.aws.s3.S3Client;
import org.apache.http.client.HttpClient;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * AWS S3 Client CDI Producer.
 *
 * @author Richard Lucas
 */
public class S3ClientProducer {

    @Inject
    @Omakase
    HttpClient httpClient;

    @Produces
    @Omakase
    public S3Client getS3Client() {
        RuntimeCredentialsProvider provider = new RuntimeCredentialsProvider();
        AmazonS3 amazonS3 = new AmazonS3Client(provider);
        return new S3Client(httpClient, provider, amazonS3);
    }
}
