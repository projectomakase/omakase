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
package org.projectomakase.omakase.repository.api;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Represents a File in a Repository.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = "omakase:repositoryFile")
public class RepositoryFile extends JcrEntity {

    @JcrProperty(name = "omakase:relativePath")
    private String relativePath;
    @JcrProperty(name = "omakase:variantId")
    private String variantId;
    @JcrProperty(name = "omakase:variantFileId")
    private String variantFileId;

    public RepositoryFile() {
        // required by Jcrom
    }

    public RepositoryFile(String variantId) {
        this.variantId = variantId;
    }

    public RepositoryFile(String relativePath, String variantId) {
        this.relativePath = relativePath;
        this.variantId = variantId;
    }

    public RepositoryFile(String relativePath, String variantId, String variantFileId) {
        this.relativePath = relativePath;
        this.variantId = variantId;
        this.variantFileId = variantFileId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getVariantFileId() {
        return variantFileId;
    }

    public void setVariantFileId(String variantFileId) {
        this.variantFileId = variantFileId;
    }

    @Override
    public String toString() {
        return "RepositoryFile{" +
                "relativePath='" + relativePath + '\'' +
                ", variantId='" + variantId + '\'' +
                ", variantFileId='" + variantFileId + '\'' +
                '}';
    }
}
