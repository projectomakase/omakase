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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link HashStrategy} implementation that calculates a Tree Hash compatible with AWS Glacier.
 *
 * @author Richard Lucas
 */
public class TreeHashStrategy extends AbstractHashStrategy {

    private static final int ONE_MB = 1024 * 1024;
    private Hasher hasher = Hashing.sha256().newHasher();
    private long bytesHashed = 0;
    private List<HashCode> hashes = new ArrayList<>();

    /**
     * Creates a new tree hash strategy that calculates a Tree Hash compatible with AWS Glacier.
     *
     * @param byteRange
     *         the byte range the hash strategy wil create a hash for, all other bytes will be discarded
     */
    public TreeHashStrategy(ByteRange byteRange) {
        super(Hashes.TREE_HASH, byteRange);
    }

    @Override
    void hashBytes(byte[] bytes, long offset, long length) {
        // bytes are hashed in one MB chunks before being collapsed into a single hash value
        if (bytesHashed < ONE_MB) {
            hasher.putBytes(bytes, 0, Ints.checkedCast(length));
        } else {
            hashes.add(hasher.hash());
            hasher = Hashing.sha256().newHasher();
            bytesHashed = 0;
            hasher.putBytes(bytes, 0, Ints.checkedCast(length));
        }
        bytesHashed += length;
    }

    @Override
    public Hash finisher() {
        hashes.add(hasher.hash());
        return new Hash(Hashes.TREE_HASH, Hashes.treeHashFromHashCodes(hashes).toString(), getByteRange().getFrom(), getByteRange().getTo() + 1);
    }
}
