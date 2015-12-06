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
package org.projectomakase.omakase.commons.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

/**
 * An {@link AWSCredentialsProvider} implementation that allows the credentials to be set at runtime.
 *
 * @author Richard Lucas
 */
public class RuntimeCredentialsProvider implements AWSCredentialsProvider {

    private static final AWSCredentials DUMMY_CREDENTIALS = new BasicAWSCredentials("access", "key");

    private AWSCredentials awsCredentials = DUMMY_CREDENTIALS;

    @Override
    public AWSCredentials getCredentials() {
        return awsCredentials;
    }

    public void setAwsCredentials(AWSCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    @Override
    public void refresh() {
        awsCredentials = DUMMY_CREDENTIALS;
    }
}
