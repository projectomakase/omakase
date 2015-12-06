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
package org.projectomakase.omakase.job.task.queue;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

/**
 * Converts Omakase priority to JMS priority.
 * <p>
 * Omakase priority is 1 (high) to 10 (low) and JMS priority is 0 (low) to 9 (high).
 * </p>
 *
 * @author Richard Lucas
 */
public class JMSPriorityConverter {

    private static final int[] MESSAGE_PRIORITIES = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};

    public int convert(long taskPriority) {
        if (!Range.closed(1L, 10L).contains(taskPriority)) {
            throw new IllegalArgumentException("Invalid task priority value '" + taskPriority + "', the value must be between 1 and 10");
        }
        int index = Ints.checkedCast(taskPriority) - 1;
        return MESSAGE_PRIORITIES[index];
    }
}
