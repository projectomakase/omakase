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
package org.projectomakase.omakase.producers;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;

/**
 * @author Richard Lucas
 */
public class MockGlacierClientProducer extends GlacierClientProducer {

    @Override
    @Specializes
    @Produces
    @Omakase
    public GlacierClient getGlacierClient() {
        return new MockGlacierClient();
    }
}
