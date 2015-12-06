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
package org.projectomakase.omakase.broker;

/**
 * The capacity available for a specific worker
 *
 * @author Richard Lucas
 */
public class Capacity {

    private final String type;
    private final int availability;

    /**
     * Capacity
     *
     * @param type
     *         the task type that the worker has capacity for.
     * @param availability
     *         the available capacity for the task type
     */
    public Capacity(String type, int availability) {
        this.type = type;
        this.availability = availability;
    }

    /**
     * Gets the task type that the worker has capacity for.
     *
     * @return the task type that the worker has capacity for.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the available capacity for the task type
     *
     * @return the available capacity for the task type
     */
    public int getAvailability() {
        return availability;
    }
}
