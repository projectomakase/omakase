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
package org.projectomakase.omakase.job.rest.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * REST Replication Job Configuration Representation
 *
 * @author Richard Lucas
 */
@JsonPropertyOrder({"variant", "source_repositories", "destination_repositories"})
public class ReplicationJobConfigurationModel implements JobConfigurationModel {

    private String variant;
    @JsonProperty("source_repositories")
    private List<String> sourceRepositories;
    @JsonProperty("destination_repositories")
    private List<String> destinationRepositories;

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public List<String> getSourceRepositories() {
        return sourceRepositories;
    }

    public void setSourceRepositories(List<String> sourceRepositories) {
        this.sourceRepositories = sourceRepositories;
    }

    public List<String> getDestinationRepositories() {
        return destinationRepositories;
    }

    public void setDestinationRepositories(List<String> destinationRepositories) {
        this.destinationRepositories = destinationRepositories;
    }
}
