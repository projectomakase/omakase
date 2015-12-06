/*
 * #%L
 * omakase-tool-manifest
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
package org.projectomakase.omakase.worker.tool.manifest.assertions;

import org.projectomakase.omakase.task.providers.manifest.ManifestFile;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.net.URI;

/**
 * @author Richard Lucas
 */
public class ManifestFileAssert extends AbstractAssert<ManifestFileAssert, ManifestFile> {

    public ManifestFileAssert(ManifestFile actual) {
        super(actual, ManifestFileAssert.class);
    }

    public ManifestFileAssert hasURI(URI uri) {
        isNotNull();
        Assertions.assertThat(actual.getUri()).isEqualTo(uri);
        return this;
    }

    public ManifestFileAssert hasSize(long size) {
        isNotNull();
        Assertions.assertThat(actual.getSize()).contains(size);
        return this;
    }
}
