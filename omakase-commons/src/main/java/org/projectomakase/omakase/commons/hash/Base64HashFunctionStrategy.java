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

import java.util.Base64;

/**
 * Hash Strategy that uses the supplied Guava {@link HashFunction} to calculate a {@link HashCode}.
 * <p>
 * Returns a base64 encoded hash.
 * </p>
 *
 * @author Richard Lucas
 */
public class Base64HashFunctionStrategy extends HashFunctionStrategy {

    public Base64HashFunctionStrategy(String algorithm, HashFunction hashFunction, ByteRange byteRange) {
        super(algorithm, hashFunction, byteRange);
    }

    @Override
    public Hash finisher() {
        HashCode hashCode = hasher.hash();
        String base64 = Base64.getEncoder().encodeToString(hashCode.asBytes());
        return new Hash(getAlgorithm(), base64, getByteRange().getFrom(), getByteRange().getTo() + 1);
    }
}
