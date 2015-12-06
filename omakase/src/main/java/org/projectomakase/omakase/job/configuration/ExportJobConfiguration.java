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
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Export Job Configuration
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:exportConfiguration"}, classNameProperty = "className")
public class ExportJobConfiguration extends JcrEntity implements JobConfiguration {

    @JcrProperty(name = "omakase:variant")
    private String variant;
    @JcrProperty(name = "omakase:repositories")
    private List<String> repositories;
    @JcrProperty(name = "omakase:locations")
    private List<String> locations;
    @JcrProperty(name = "omakase:validate")
    private boolean validate;

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

    public List<String> getLocations() {
        return Optional.ofNullable(locations).orElse(new ArrayList<>());
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public boolean getValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }


    @Override
    public void validate(JobConfigurationValidator validator) {
        validator.validate(this);
    }

    @Override
    public String toString() {
        return "ExportJobConfiguration{" +
                "variant='" + variant + '\'' +
                ", repositories=" + repositories +
                ", locations=" + locations +
                ", validate=" + validate +
                '}';
    }

    public static class Builder {
        @FunctionalInterface
        public interface JobConfigurationSetter extends Consumer<ExportJobConfiguration> {
        }

        public static ExportJobConfiguration build(JobConfigurationSetter... jobSetters) {
            final ExportJobConfiguration job = new ExportJobConfiguration();

            Stream.of(jobSetters).forEach(s -> s.accept(job));

            return job;
        }
    }
}
