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
package org.projectomakase.omakase.repository.provider.glacier;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Glacier Repository Configuration.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {GlacierRepositoryConfiguration.MIXIN}, classNameProperty = "className")
public class GlacierRepositoryConfiguration extends JcrEntity implements RepositoryConfiguration {

    public static final String MIXIN = "omakase:repositoryGlacierConfiguration";

    @JcrProperty(name = "omakase:awsAccessKey")
    private String awsAccessKey;
    @JcrProperty(name = "omakase:awsSecretKey")
    private String awsSecretKey;
    @JcrProperty(name = "omakase:region")
    private String region;
    @JcrProperty(name = "omakase:vault")
    private String vault;
    @JcrProperty(name = "omakase:snsTopicArn")
    private String snsTopicArn;

    /**
     * Empty constructor
     */
    public GlacierRepositoryConfiguration() {
        // required by Jcrom
    }

    public GlacierRepositoryConfiguration(String awsAccessKey, String awsSecretKey, String region, String vault, String snsTopicArn) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.region = region;
        this.vault = vault;
        this.snsTopicArn = snsTopicArn;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getVault() {
        return vault;
    }

    public void setVault(String vault) {
        this.vault = vault;
    }

    public String getSnsTopicArn() {
        return snsTopicArn;
    }

    public void setSnsTopicArn(String snsTopicArn) {
        this.snsTopicArn = snsTopicArn;
    }
}
