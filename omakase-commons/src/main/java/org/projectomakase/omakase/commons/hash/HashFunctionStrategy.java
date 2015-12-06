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
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.primitives.Ints;

/**
 * Hash Strategy that uses the supplied Guava {@link HashFunction} to calculate a {@link HashCode}
 *
 * @author Richard Lucas
 */
public class HashFunctionStrategy extends AbstractHashStrategy {

    final Hasher hasher;

    /**
     * Creates a new hash function hash strategy that uses the supplied Guava {@link HashFunction}
     *
     * @param algorithm
     *         the hash algorithm used by the strategy
     * @param hashFunction
     *         a Guava {@link HashFunction}
     * @param byteRange
     *         the byte range the hash strategy wil create a hash for, all other bytes will be discarded
     */
    public HashFunctionStrategy(String algorithm, HashFunction hashFunction, ByteRange byteRange) {
        super(algorithm, byteRange);
        this.hasher = hashFunction.newHasher();
    }

    @Override
    void hashBytes(byte[] bytes, long offset, long length) {
        hasher.putBytes(bytes, Ints.checkedCast(offset), Ints.checkedCast(length));
    }

    @Override
    public Hash finisher() {
        return new Hash(getAlgorithm(), hasher.hash().toString(), getByteRange().getFrom(), getByteRange().getTo() + 1);
    }
}
