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
package org.projectomakase.omakase.worker.tool.manifest;

import org.projectomakase.omakase.task.providers.manifest.Manifest;
import org.projectomakase.omakase.task.providers.manifest.ManifestFile;

import java.util.List;

/**
 * Contains the results of parsing a manifest.
 *
 * @author Richard Lucas
 */
public class ManifestParserResult {

    private final List<Manifest> manifests;
    private final List<ManifestFile> files;

    /**
     * Creates a new {@link ManifestParserResult}
     *
     * @param manifests
     *         a list of manifests referenced by the parsed manifest
     * @param files
     *         a list of file URI's referenced by the parsed manifest
     */
    public ManifestParserResult(List<Manifest> manifests, List<ManifestFile> files) {
        this.manifests = manifests;
        this.files = files;
    }

    public List<Manifest> getManifests() {
        return manifests;
    }

    public List<ManifestFile> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        return "ManifestParserResult{" +
                "manifests=" + manifests +
                ", files=" + files +
                '}';
    }
}
