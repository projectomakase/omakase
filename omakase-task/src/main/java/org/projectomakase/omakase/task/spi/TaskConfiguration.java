/*
 * #%L
 * omakase-task
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
package org.projectomakase.omakase.task.spi;

/**
 * Task configuration. The configuration is type specific.
 *
 * @author Richard Lucas
 */
public interface TaskConfiguration {

    /**
     * Returns the task type the configuration is associated with.
     *
     * @return the task type the configuration is associated with.
     */
    String getTaskType();

    /**
     * Serializes the Task configuration into a JSON string.
     *
     * @return a JSON representation of the task configuration.
     */
    String toJson();

    /**
     * Populates the configuration with the the specified JSON.
     *
     * @param json
     *         the JSON representation of the configuration.
     */
    void fromJson(String json);
}
