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
import com.google.common.io.ByteProcessor;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import java.io.IOException;
import java.util.List;

/**
 * @author Richard Lucas
 */
public class HashByteProcessor implements ByteProcessor<ImmutableList<Hash>> {

    private final List<HashStrategy> hashStrategies;

    public HashByteProcessor(List<HashStrategy> hashStrategies) {
        this.hashStrategies = hashStrategies;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean processBytes(byte[] buf, int off, int len) throws IOException {
        hashStrategies.forEach(hashStrategy -> hashStrategy.readBytes(buf, len));
        return true;
    }

    @Override
    public ImmutableList<Hash> getResult() {
        return hashStrategies.stream().map(HashStrategy::finisher).collect(ImmutableListCollector.toImmutableList());
    }
}
