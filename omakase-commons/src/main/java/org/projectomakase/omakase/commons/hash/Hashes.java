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

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.functions.TailCall;
import org.projectomakase.omakase.commons.functions.TailCalls;

import java.util.Iterator;
import java.util.List;

import static org.projectomakase.omakase.commons.collectors.ImmutableListCollector.toImmutableList;

/**
 * Hashing utility methods.
 *
 * @author Richard Lucas
 */
public final class Hashes {

    public static final String MD5 = "MD5";
    public static final String MD5_BASE64 = "MD5_BASE64";
    public static final String SHA1 = "SHA1";
    public static final String SHA256 = "SHA256";
    public static final String SHA512 = "SHA512";
    public static final String CRC32C = "CRC32C";
    public static final String CRC32 = "CRC32";
    public static final String TREE_HASH = "TREE_HASH";

    private Hashes() {
        // hides default constructor
    }

    /**
     * Returns the {@link HashStrategy} for the given hash algorithm.
     *
     * @param hashAlgorithm
     *         the hash algorithm
     * @param byteRange
     *         the byte range the hash strategy should calculate a hash for
     * @return the {@link HashStrategy} for the given hash algorithm.
     * @throws IllegalArgumentException
     *         if the hash algorithm is not supported.
     */
    public static HashStrategy getHashStrategy(String hashAlgorithm, ByteRange byteRange) {
        switch (hashAlgorithm.toUpperCase()) {
            case MD5:
                return new HashFunctionStrategy(hashAlgorithm, Hashing.md5(), byteRange);
            case MD5_BASE64:
                return new Base64HashFunctionStrategy(hashAlgorithm, Hashing.md5(), byteRange);
            case SHA1:
                return new HashFunctionStrategy(hashAlgorithm, Hashing.sha1(), byteRange);
            case SHA256:
                return new HashFunctionStrategy(hashAlgorithm, Hashing.sha256(), byteRange);
            case SHA512:
                return new HashFunctionStrategy(hashAlgorithm, Hashing.sha512(), byteRange);
            case CRC32C:
                return new HashFunctionStrategy(hashAlgorithm, Hashing.crc32c(), byteRange);
            case CRC32:
                return new HashFunctionStrategy(hashAlgorithm, Hashing.crc32(), byteRange);
            case TREE_HASH:
                return new TreeHashStrategy(byteRange);
            default:
                throw new IllegalArgumentException("Unsupported hash algorithm " + hashAlgorithm);
        }
    }

    /**
     * Returns a {@link HashCode} representing a tree hash calculated from a list of hash codes.
     * <p>
     * This uses the algorithm documented here http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html to calculate
     * each level of the tree until only a single value remains.
     * </p>
     *
     * @param hashes
     *         the list of hashes
     * @return a {@link HashCode} representing the tree hash.
     */
    public static HashCode treeHashFromHashCodes(List<HashCode> hashes) {
        return hashLevel(hashes).invoke().stream().findFirst().get();
    }

    /**
     * Returns a {@link HashCode} representing a tree hash calculated from a list of hexadecimal ({@code base 16}) encoded strings.
     * <p>
     * This uses the algorithm documented here http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html to calculate
     * each level of the tree until only a single value remains.
     * </p>
     *
     * @param hashes
     *         the list of hashes
     * @return a {@link HashCode} representing the tree hash.
     */
    public static HashCode treeHashFromStrings(List<String> hashes) {
        return hashLevel(hashes.stream().map(HashCode::fromString).collect(toImmutableList())).invoke().stream().findFirst().get();
    }

    /**
     * Creates a list of {@link ByteRange} values for the given content size and part size.
     *
     * @param partSize
     *         the part size used to spilt the content
     * @param contentSize
     *         the content size
     * @return a list of {@link ByteRange} values for the given content size and part size.
     */
    public static List<ByteRange> createByteRanges(long partSize, long contentSize) {
        long parts = contentSize / partSize + (contentSize % partSize == 0 ? 0 : 1);
        return ContiguousSet.create(Range.closedOpen(0L, parts), DiscreteDomain.longs()).stream().map(p -> new ByteRange(partSize * p, Math.min(partSize * (p + 1) - 1, contentSize - 1)))
                .collect(ImmutableListCollector.toImmutableList());

    }

    private static TailCall<ImmutableList<HashCode>> hashLevel(List<HashCode> hashes) {
        // Recursively hash pairs of values in the level and add them to the result list, repeat until only a single hash remains.
        ImmutableList.Builder<HashCode> resultBuilder = ImmutableList.builder();
        for (Iterator<HashCode> iterator = hashes.iterator(); iterator.hasNext(); ) {
            HashCode hashCode1 = iterator.next();
            if (iterator.hasNext()) {
                HashCode hashCode2 = iterator.next();
                resultBuilder.add(Hashing.sha256().newHasher().putBytes(hashCode1.asBytes()).putBytes(hashCode2.asBytes()).hash());
            } else {
                resultBuilder.add(hashCode1);
            }
        }
        ImmutableList<HashCode> result = resultBuilder.build();
        if (result.size() == 1) {
            return TailCalls.done(result);
        } else {
            return TailCalls.call(() -> hashLevel(result));
        }
    }
}
