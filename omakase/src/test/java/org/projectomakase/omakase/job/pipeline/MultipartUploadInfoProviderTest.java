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
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class MultipartUploadInfoProviderTest {

    private MultipartUploadInfoProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new MultipartUploadInfoProvider();
        provider.s3PartSize = 10;
        provider.glacierPartSize = 20;
    }

    @Test
    public void shouldGetS3MultipartUploadInfo() throws Exception {
        Optional<MultipartUploadInfo> multipartUploadInfo = provider.get(new URI("s3://s3.amazonaws.com"));
        assertThat(multipartUploadInfo).isPresent();
        assertThat(multipartUploadInfo.get()).isEqualToComparingFieldByField(new MultipartUploadInfo(10, ImmutableList.of(Hashes.SHA256, Hashes.MD5_BASE64)));
    }

    @Test
    public void shouldGetGlacierMultipartUploadInfo() throws Exception {
        Optional<MultipartUploadInfo> multipartUploadInfo = provider.get(new URI("glacier://glacier.amazonaws.com"));
        assertThat(multipartUploadInfo).isPresent();
        assertThat(multipartUploadInfo.get()).isEqualToComparingFieldByField(new MultipartUploadInfo(20, ImmutableList.of(Hashes.SHA256, Hashes.TREE_HASH)));
    }

    @Test
    public void shouldGetEmptyOptional() throws Exception {
        assertThat(provider.get(new URI("unknown://glacier.amazonaws.com"))).isEmpty();
    }
}