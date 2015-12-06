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
package org.projectomakase.omakase.content;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = "omakase:variantRepository")
public class VariantRepository extends JcrEntity {

    public static final String REPOSITORY_NAME = "omakase:repositoryName";
    public static final String TYPE = "omakase:repositoryType";

    @JcrProperty(name = REPOSITORY_NAME)
    private String repositoryName;
    @JcrProperty(name = TYPE)
    private String type;

    public VariantRepository() {
        //required by JCROM
    }

    public VariantRepository(String repositoryId, String repositoryName, String type) {
        this.name = repositoryId;
        this.repositoryName = repositoryName;
        this.type = type;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "VariantRepository{" +
                "id='" + name + '\'' +
                "repositoryName='" + repositoryName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
