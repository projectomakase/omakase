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
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Delete Variant Job Configuration.
 * <p>
 * This is an internal job used to delete a variant and it's associated files.
 * </p>
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:deleteVariantConfiguration"}, classNameProperty = "className")
public class DeleteVariantJobConfiguration extends JcrEntity implements JobConfiguration {

    @JcrProperty(name = "omakase:variant")
    private String variant;
    @JcrProperty(name = "omakase:repositories")
    private List<String> repositories;

    public DeleteVariantJobConfiguration() {
        // required by JCROM
    }

    public DeleteVariantJobConfiguration(String variant, List<String> repositories) {
        this.variant = variant;
        this.repositories = repositories;
    }

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

    @Override
    public void validate(JobConfigurationValidator validator) {
        // no-op
    }

    @Override
    public String toString() {
        return "DeleteVariantJobConfiguration{" +
                "variant='" + variant + '\'' +
                ", repositories=" + repositories +
                '}';
    }
}
