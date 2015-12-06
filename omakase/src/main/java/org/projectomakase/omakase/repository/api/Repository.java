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
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Represents an Omakase repository.
 *
 * @author Scott Sharp
 */
@JcrNode(mixinTypes = {"omakase:repository"})
public class Repository extends JcrEntity {

    public static final String REPOSITORY_NAME = "omakase:name";
    public static final String DESCRIPTION = "omakase:description";
    public static final String TYPE = "omakase:type";

    @JcrProperty(name = REPOSITORY_NAME)
    private String repositoryName;
    @JcrProperty(name = DESCRIPTION)
    private String description;
    @JcrProperty(name = Repository.TYPE)
    private String type;
    @JcrChildNode(createContainerNode = false)
    private RepositoryConfiguration repositoryConfiguration;

    public Repository() {
        // required by Jcrom
    }

    public Repository(String repositoryName, String description, String type) {
        this.repositoryName = repositoryName;
        this.description = description;
        this.type = type;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RepositoryConfiguration getRepositoryConfiguration() {
        return repositoryConfiguration;
    }

    public void setRepositoryConfiguration(RepositoryConfiguration repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }
}
