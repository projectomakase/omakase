/*
 * #%L
 * omakase-task
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
package org.projectomakase.omakase.task.providers.aws.s3;

import org.projectomakase.omakase.commons.aws.AWSUploadPart;
import org.projectomakase.omakase.task.providers.aws.AWSUploadTaskConfiguration;

import java.net.URI;
import java.util.List;

/**
 * AWS Glacier Upload Task Configuration.
 *
 * @author Richard Lucas
 */
public class S3UploadTaskConfiguration extends AWSUploadTaskConfiguration {

    private static final String TASK_TYPE = "S3_UPLOAD";

    public S3UploadTaskConfiguration() {
        // required by service loader
    }

    public S3UploadTaskConfiguration(URI source, URI destination, long partSize, List<AWSUploadPart> parts, List<String> hashAlgorithms) {
        super(source, destination, partSize, parts, hashAlgorithms);
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String toString() {
        return "S3UploadTaskConfiguration{" +
                "source=" + getSource() +
                ", destination=" + getDestination() +
                ", partSize=" + getPartSize() +
                ", parts=" + getParts() +
                ", hashAlgorithms=" + getHashAlgorithms() +
                '}';
    }
}
