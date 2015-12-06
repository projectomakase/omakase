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
package org.projectomakase.omakase;

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Represents the Omakase Cluster this Omakase Node belongs to.
 *
 * @author Richard Lucas
 */
@ApplicationScoped
public class OmakaseCluster {

    @Inject
    @ConfigProperty(name = "omakase.cluster.name", defaultValue = "omakase")
    String clusterName;

    /**
     * Returns the name of the Omakase Cluster.
     *
     * @return the name of the Omakase Cluster.
     */
    public String getClusterName() {
        return clusterName;
    }
}
