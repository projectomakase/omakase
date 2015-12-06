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
package org.projectomakase.omakase.repository.provider.s3;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * S3 Repository Configuration.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {S3RepositoryConfiguration.MIXIN}, classNameProperty = "className")
public class S3RepositoryConfiguration extends JcrEntity implements RepositoryConfiguration {

    public static final String MIXIN = "omakase:repositoryS3Configuration";

    @JcrProperty(name = "omakase:awsAccessKey")
    private String awsAccessKey;
    @JcrProperty(name = "omakase:awsSecretKey")
    private String awsSecretKey;
    @JcrProperty(name = "omakase:region")
    private String region;
    @JcrProperty(name = "omakase:bucket")
    private String bucket;
    @JcrProperty(name = "omakase:root")
    private String root;

    /**
     * Empty constructor
     */
    public S3RepositoryConfiguration() {
        // required by Jcrom
    }

    public S3RepositoryConfiguration(String awsAccessKey, String awsSecretKey, String region, String bucket, String root) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.region = region;
        this.bucket = bucket;
        this.root = root;
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

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
