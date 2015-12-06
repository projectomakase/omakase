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
package org.projectomakase.omakase.job.queue.provider;

import org.projectomakase.omakase.job.task.queue.JMSPriorityConverter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class JMSPriorityConverterTest {

    JMSPriorityConverter converter = new JMSPriorityConverter();

    @Test
    public void shouldConvertTaskToMessagePriority() {
        assertThat(converter.convert(1)).isEqualTo(9);
        assertThat(converter.convert(2)).isEqualTo(8);
        assertThat(converter.convert(3)).isEqualTo(7);
        assertThat(converter.convert(4)).isEqualTo(6);
        assertThat(converter.convert(5)).isEqualTo(5);
        assertThat(converter.convert(6)).isEqualTo(4);
        assertThat(converter.convert(7)).isEqualTo(3);
        assertThat(converter.convert(8)).isEqualTo(2);
        assertThat(converter.convert(9)).isEqualTo(1);
        assertThat(converter.convert(10)).isEqualTo(0);
    }

    @Test
    public void shouldThrowExceptionIfTaskPriorityIsOutOfRange() {
        try {
            converter.convert(-1);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Invalid task priority value '-1', the value must be between 1 and 10");
        }

        try {
            converter.convert(11);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("Invalid task priority value '11', the value must be between 1 and 10");
        }
    }

}