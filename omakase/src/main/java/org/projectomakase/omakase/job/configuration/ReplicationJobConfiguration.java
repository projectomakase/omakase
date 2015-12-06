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
 * Ingest Job Configuration
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:replicationConfiguration"}, classNameProperty = "className")
public class ReplicationJobConfiguration extends JcrEntity implements JobConfiguration {

    @JcrProperty(name = "omakase:variant")
    private String variant;
    @JcrProperty(name = "omakase:sourceRepositories")
    private List<String> sourceRepositories;
    @JcrProperty(name = "omakase:destinationRepositories")
    private List<String> destinationRepositories;

    @Override
    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public List<String> getSourceRepositories() {
        return Optional.ofNullable(sourceRepositories).orElse(new ArrayList<>());
    }

    public void setSourceRepositories(List<String> sourceRepositories) {
        this.sourceRepositories = sourceRepositories;
    }

    public List<String> getDestinationRepositories() {
        return Optional.ofNullable(destinationRepositories).orElse(new ArrayList<>());
    }

    public void setDestinationRepositories(List<String> destinationRepositories) {
        this.destinationRepositories = destinationRepositories;
    }

    @Override
    public void validate(JobConfigurationValidator validator) {
        validator.validate(this);
    }

    @Override
    public String toString() {
        return "ReplicationJobConfiguration{" +
                "variant='" + variant + '\'' +
                ", sourceRepositories=" + sourceRepositories +
                ", destinationRepositories=" + destinationRepositories +
                '}';
    }

    public static class Builder {
        @FunctionalInterface
        public interface JobConfigurationSetter extends Consumer<ReplicationJobConfiguration> {
        }

        public static ReplicationJobConfiguration build(JobConfigurationSetter... jobSetters) {
            final ReplicationJobConfiguration configuration = new ReplicationJobConfiguration();

            Stream.of(jobSetters).forEach(s -> s.accept(configuration));

            return configuration;
        }
    }
}


