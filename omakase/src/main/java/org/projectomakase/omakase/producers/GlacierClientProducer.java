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

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.aws.RuntimeCredentialsProvider;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;
import org.apache.http.client.HttpClient;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * AWS Glacier Client CDI Producer.
 *
 * @author Richard Lucas
 */
public class GlacierClientProducer {

    @Inject
    @Omakase
    HttpClient httpClient;

    @Produces
    @Omakase
    public GlacierClient getGlacierClient() {
        RuntimeCredentialsProvider provider = new RuntimeCredentialsProvider();
        AmazonGlacier amazonGlacier = new AmazonGlacierClient(provider);
        return new GlacierClient(httpClient, provider, amazonGlacier);
    }
}
