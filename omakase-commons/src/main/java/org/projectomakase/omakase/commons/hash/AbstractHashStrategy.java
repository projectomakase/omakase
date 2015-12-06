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

import com.google.common.hash.HashFunction;

/**
 * @author Richard Lucas
 */
public abstract class AbstractHashStrategy implements HashStrategy {

    private final String algorithm;
    private final ByteRange byteRange;
    private long bytesRead = 0;

    /**
     * Creates a new hash function hash strategy that uses the supplied Guava {@link HashFunction}
     *
     * @param algorithm
     *         the hash algorithm used by the strategy
     * @param byteRange
     *         the byte range the hash strategy wil create a hash for, all other bytes will be discarded
     */
    public AbstractHashStrategy(String algorithm, ByteRange byteRange) {
        this.algorithm = algorithm;
        this.byteRange = byteRange;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public ByteRange getByteRange() {
        return byteRange;
    }

    @Override
    public HashStrategy readBytes(byte[] bytes, int length) {
        long byteRangeLength = byteRange.getTo() + 1;
        long requiredBytes = bytesRead + length + 1;

        // determines if the current byte array is within the byte range we are interested in
        boolean shouldHashBytes = (bytesRead >= byteRange.getFrom() || bytesRead + length - 1 >= byteRange.getFrom()) && bytesRead <= byteRangeLength;

        if (shouldHashBytes) {
            long offset = 0;
            long bytesToRead = length;

            // deals with the case where we need to start reading partway through the byte array
            if (bytesRead < byteRange.getFrom()) {
                offset = byteRange.getFrom() - bytesRead;
                bytesToRead -= offset;
            }

            // deals with the case where we need to stop reading before the end of the byte array
            if (requiredBytes > byteRange.getTo()) {
                bytesToRead = byteRangeLength - bytesRead - offset;
            }
            hashBytes(bytes, offset, bytesToRead);

        }
        bytesRead += length;
        return this;
    }

    abstract void hashBytes(byte[] bytes, long offset, long length);
}
