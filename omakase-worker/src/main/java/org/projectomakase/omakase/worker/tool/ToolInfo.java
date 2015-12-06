/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker.tool;

/**
 * Tool Information
 *
 * @author Richard Lucas
 */
public class ToolInfo {

    private final String name;
    private final int maxCapacity;
    private final int availableCapacity;

    public ToolInfo(String name, int maxCapacity, int availableCapacity) {
        this.name = name;
        this.maxCapacity = maxCapacity;
        this.availableCapacity = availableCapacity;
    }

    /**
     * Returns the tools name.
     *
     * @return the tools name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the tools maximum capacity i.e. the maximum number of tasks it can process concurrently.
     *
     * @return the tools maximum capacity i.e. the maximum number of tasks it can process concurrently.
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Returns the tools available capacity.
     *
     * @return the tools maximum capacity.
     */
    public int getAvailableCapacity() {
        return availableCapacity;
    }

    @Override
    public String toString() {
        return "ToolInfo{" +
                "name='" + name + '\'' +
                ", maxCapacity=" + maxCapacity +
                ", availableCapacity=" + availableCapacity +
                '}';
    }
}
