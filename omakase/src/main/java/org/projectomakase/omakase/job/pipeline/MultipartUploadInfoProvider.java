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
package org.projectomakase.omakase.job.pipeline;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;
import java.net.URI;
import java.util.Optional;

/**
 * {@link MultipartUploadInfo} Provider.
 *
 * @author Richard Lucas
 */
public class MultipartUploadInfoProvider {

    @Inject
    @ConfigProperty(name = "omakase.s3.upload.part.size")
    long s3PartSize;
    @Inject
    @ConfigProperty(name = "omakase.glacier.upload.part.size")
    long glacierPartSize;

    /**
     * Returns the {@link MultipartUploadInfo} for the given location or an empty optional if the location is not supported.
     *
     * @param location the upload location
     * @return the {@link MultipartUploadInfo} for the given location or an empty optional if the location is not supported.
     */
    public Optional<MultipartUploadInfo> get(URI location) {
        String scheme = location.getScheme();
        if ("s3".equals(scheme)) {
            return Optional.of(getS3MultipartUploadInfo());
        } else if ("glacier".equals(scheme)) {
            return Optional.of(getGlacierMultipartUploadInfo());
        } else {
            return Optional.empty();
        }
    }

    private MultipartUploadInfo getS3MultipartUploadInfo() {
        return new MultipartUploadInfo(s3PartSize, ImmutableList.of(Hashes.SHA256, Hashes.MD5_BASE64));
    }

    private MultipartUploadInfo getGlacierMultipartUploadInfo() {
        return new MultipartUploadInfo(glacierPartSize, ImmutableList.of(Hashes.SHA256, Hashes.TREE_HASH));
    }
}
