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
package org.projectomakase.omakase.job.configuration;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Ingest Job Configuration
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:ingestConfiguration"}, classNameProperty = "className")
public class IngestJobConfiguration extends JcrEntity implements JobConfiguration {

    private static final String HLS = ".m3u8";
    private static final String DASH = ".mpd";
    private static final String SMOOTH = ".ism";

    @JcrProperty(name = "omakase:variant")
    private String variant;
    @JcrProperty(name = "omakase:repositories")
    private List<String> repositories;
    @JcrProperty(name = "omakase:deleteSource")
    private boolean deleteSource;
    @JcrChildNode
    private List<IngestJobFile> ingestJobFiles;

    @Override
    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public List<String> getRepositories() {
        return Optional.ofNullable(repositories).orElse(new ArrayList<>());
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }

    public boolean getDeleteSource() {
        return deleteSource;
    }

    public void setDeleteSource(boolean deleteSource) {
        this.deleteSource = deleteSource;
    }

    public List<IngestJobFile> getIngestJobFiles() {
        return Optional.ofNullable(ingestJobFiles).orElse(new ArrayList<>());
    }

    public void setIngestJobFiles(List<IngestJobFile> ingestJobFiles) {
        this.ingestJobFiles = ingestJobFiles;
    }

    @Override
    public void validate(JobConfigurationValidator validator) {
        validator.validate(this);
    }

    @Override
    public String toString() {
        return "IngestJobConfiguration{" +
                "variant='" + variant + '\'' +
                ", repositories=" + repositories +
                ", ingestJobFiles=" + ingestJobFiles +
                '}';
    }

    public boolean isManifestIngest() {
        return getIngestJobFiles().stream().filter(IngestJobFile::getIsManifest).findFirst().isPresent();
    }

    public Optional<ManifestType> getManifestType() {
        return getIngestJobFiles().stream().filter(IngestJobFile::getIsManifest).findFirst().flatMap(IngestJobConfiguration::getManifestType);
    }

    private static Optional<ManifestType> getManifestType(IngestJobFile ingestJobFile) {
        Optional<ManifestType> manifestType;
        if (ingestJobFile.getUri().toLowerCase().endsWith(HLS)) {
            manifestType = Optional.of(ManifestType.HLS);
        } else if (ingestJobFile.getUri().toLowerCase().endsWith(DASH)) {
            manifestType = Optional.of(ManifestType.DASH);
        } else if (ingestJobFile.getUri().toLowerCase().endsWith(SMOOTH)) {
            manifestType = Optional.of(ManifestType.SMOOTH);
        } else {
            manifestType = Optional.empty();
        }

        return manifestType;
    }

    public static class Builder {
        @FunctionalInterface
        public interface JobConfigurationSetter extends Consumer<IngestJobConfiguration> {
        }

        public static IngestJobConfiguration build(JobConfigurationSetter... jobSetters) {
            final IngestJobConfiguration job = new IngestJobConfiguration();

            Stream.of(jobSetters).forEach(s -> s.accept(job));

            return job;
        }
    }
}
