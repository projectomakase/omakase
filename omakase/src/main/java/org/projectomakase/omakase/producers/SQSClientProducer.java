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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * CDI Producer for a {@link AmazonSQSClient}. The client is configured via Omakase configuration properties.
 *
 * @author Richard Lucas
 */
public class SQSClientProducer {

    @Inject
    @ConfigProperty(name = "omakase.sqs.access.key")
    String accessKey;
    @Inject
    @ConfigProperty(name = "omakase.sqs.secret.key")
    String secretKey;
    @Inject
    @ConfigProperty(name = "omakase.sqs.region", defaultValue = "us-west-1")
    String region;

    @Produces
    @Named("sqsClient")
    public AmazonSQSClient getSQSClient() {
        AmazonSQSClient client = new AmazonSQSClient(new BasicAWSCredentials(accessKey, secretKey));
        client.setRegion(Region.getRegion(Regions.fromName(region)));
        return client;
    }
}
