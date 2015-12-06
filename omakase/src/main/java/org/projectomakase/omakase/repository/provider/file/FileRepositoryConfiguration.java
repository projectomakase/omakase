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
package org.projectomakase.omakase.repository.provider.file;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Local File System Repository Configuration.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {FileRepositoryConfiguration.MIXIN}, classNameProperty = "className")
public class FileRepositoryConfiguration extends JcrEntity implements RepositoryConfiguration {

    public static final String MIXIN = "omakase:repositoryFileConfiguration";

    @JcrProperty(name = "omakase:root")
    private String root;

    /**
     * Empty constructor
     */
    public FileRepositoryConfiguration() {
        // required by Jcrom
    }

    /**
     * Creates a new File Repository Configuration instance.
     *
     * @param root
     *         the root uri of the filesystem repository
     */
    public FileRepositoryConfiguration(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
