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
package org.projectomakase.omakase.camel;

import org.apache.camel.CamelContext;

/**
 * {@link CamelQueueEndpoint} implementation for AWS SQS.
 *
 * @author Richard Lucas
 */
public class SQSCamelQueueEndpoint implements CamelQueueEndpoint {

    private static final String PROVIDER_NAME = "SQS";
    private static final String QUEUE_ENDPOINT = "aws-sqs://%s?AmazonSQSClient=#sqsClient";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public void register(CamelContext camelContext) {
        // no-op
    }

    @Override
    public String getQueueEndpoint(String queueName) {
        return String.format(QUEUE_ENDPOINT, queueName);
    }
}
