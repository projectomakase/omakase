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

import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a file being ingesting into Omakase via the Ingest Job.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:ingestFile"})
public class IngestJobFile extends JcrEntity {

    @JcrProperty(name = "omakase:uri")
    private String uri;
    @JcrProperty(name = "omakase:size")
    private Long size;
    @JcrProperty(name = "omakase:isManifest")
    private Boolean isManifest = false;
    @JcrProperty(name = "omakase:hash")
    private String hash;
    @JcrProperty(name = "omakase:hashAlgorithm")
    private String hashAlgorithm;

    public IngestJobFile() {
        // auto assigns a new id when creating the object, this is used when creating the JCR node.
        // This will be overridden by JCROM when retrieving the object from an existing JCR node with the existing id.
        IdGenerator idGenerator = new IdGenerator();
        super.name = idGenerator.getId();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getIsManifest() {
        return isManifest;
    }

    public void setIsManifest(Boolean isManifest) {
        this.isManifest = isManifest;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public String toString() {
        return "IngestJobFile{" +
                "uri='" + uri + '\'' +
                ", size=" + size +
                ", isManifest=" + isManifest +
                ", hash='" + hash + '\'' +
                ", hashAlgorithm='" + hashAlgorithm + '\'' +
                '}';
    }

    public static class Builder {
        @FunctionalInterface
        public interface IngestJobFileSetter extends Consumer<IngestJobFile> {
        }

        public static IngestJobFile build(IngestJobFileSetter... jobSetters) {
            final IngestJobFile ingestJobFile = new IngestJobFile();

            Stream.of(jobSetters).forEach(s -> s.accept(ingestJobFile));

            return ingestJobFile;
        }
    }
}
