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

import org.projectomakase.omakase.task.api.Task;

/**
 * Tool interface.
 *
 * @author Richard Lucas
 */
public interface Tool {

    /**
     * Executes the task.
     *
     * @param task the task to execute
     */
    void execute(Task task);

    /**
     * Returns the tool's name
     *
     * @return the tool's name
     */
    String getName();
}
