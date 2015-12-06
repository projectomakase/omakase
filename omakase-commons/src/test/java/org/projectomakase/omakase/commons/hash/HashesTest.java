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
package org.projectomakase.omakase.commons.hash;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.file.FileGenerator;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class HashesTest {

    private static final String EXPECTED_TREE_HASH = "aaf178963dba5142d81e6feda72bfaf1c7064df2276672ec9295326367444ce4";
    private static final List<String> HASHES = ImmutableList.of("5bfea26ced5ed2670589c3e3549413dbe74e1ca23af296374ea9307ac5e79101", "73f4b2725be9db345d9b3aa14345cecf9fd74e469ebc8160c5def7fb6a1140fa",
            "907cf5c07eb70772ec9dfdf734ab286de9fd9f92651c424804dc0e13b25be507", "17f8d4b30e9cdf5a69cecbc02f7da310ec81cebddf82a2c4a11f057f18b3ad6e",
            "7d85dc06c94fafca4d24277054f94ed7313a8eb612c966cf91d82270c864c338");
    private static final long ONE_MB = 1024 * 1024;
    private static final long FOUR_MB = ONE_MB * 4;
    private static final ByteRange BYTE_RANGE = new ByteRange(0, 4195074);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldCreateSHA256Hash() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.SHA256, BYTE_RANGE), FOUR_MB, "593aae576b3cfc6ad474600343c26f822144087f712572b08fa56c3beabf7f18");
    }

    @Test
    public void shouldCreateSHA256HashForByteRange() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.SHA256, new ByteRange(2097152, 3145727)), FOUR_MB, "907cf5c07eb70772ec9dfdf734ab286de9fd9f92651c424804dc0e13b25be507");
    }

    @Test
    public void shouldCreateMD5Hash() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.MD5, BYTE_RANGE), FOUR_MB, "e2325cb8e030bff536aa278ef6b699ef");
    }

    @Test
    public void shouldCreateTreeHash() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.TREE_HASH, BYTE_RANGE), FOUR_MB, "aaf178963dba5142d81e6feda72bfaf1c7064df2276672ec9295326367444ce4");
    }

    @Test
    public void shouldCreateTreeHashForByteRange() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.TREE_HASH, new ByteRange(0, 1573249)), FOUR_MB, "a9a796a3c056300817ac84296e0902c9862ae21f0c5e1f9af371a0872e00bde1");
    }

    @Test
    public void shouldCreateHashForByteRangeThatStartsPartwayThroughTheBufferAndReadsTheWholeBuffer() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.SHA256, new ByteRange(4000, 4097)), 5000, "93f6f3bd373f48a2ee16139193c9b048a4d44009e5cce559f90337665ce6d70d");
    }

    @Test
    public void shouldCreateHashForByteRangeThatStartsAndEndsPartwayThroughTheBuffer() throws Exception {
        validateHashStrategy(Hashes.getHashStrategy(Hashes.SHA256, new ByteRange(4000, 4095)), 4096, "b0a3e18ee75651474c175e2b09538cc3a6ae7d6cd34e40ed5c1d7e858a9d5959");
    }

    @Test
    public void shouldCalculateTreeHashFromStrings() throws Exception {
        Assertions.assertThat(Hashes.treeHashFromStrings(HASHES).toString()).isEqualTo(EXPECTED_TREE_HASH);
    }

    @Test
    public void shouldCalculateTreeHashFromHashCodes() throws Exception {
        assertThat(Hashes.treeHashFromHashCodes(HASHES.stream().map(HashCode::fromString).collect(ImmutableListCollector.toImmutableList())).toString()).isEqualTo(EXPECTED_TREE_HASH);
    }

    @Test
    public void shouldCreateByteRanges() throws Exception {
        assertThat(Hashes.createByteRanges(ONE_MB, 4195075)).extracting("from", "to")
                .contains(new Tuple(0L, 1048575L), new Tuple(1048576L, 2097151L), new Tuple(2097152L, 3145727L), new Tuple(3145728L, 4194303L), new Tuple(4194304L, 4195074L));
    }

    @Test
    public void shouldCreateByteRangesContentSizeSmallerThanPartSize() throws Exception {
        assertThat(Hashes.createByteRanges(4195075, ONE_MB)).extracting("from", "to").contains(new Tuple(0L, 1048575L));
    }

    private void validateHashStrategy(HashStrategy hashStrategy, long testFileSize, String expectedHash) throws Exception {
        File testFile = FileGenerator.generate(temporaryFolder.getRoot(), testFileSize);
        try (FileInputStream inputStream = new FileInputStream(testFile)) {
            List<Hash> hash = ByteStreams.readBytes(inputStream, new HashByteProcessor(ImmutableList.of(hashStrategy)));
            assertThat(hash.get(0).getValue()).isEqualTo(expectedHash);
        }
    }
}