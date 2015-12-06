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

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.task.api.Task;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class ToolRegistryTest {

    private ToolRegistry toolRegistry;

    @Before
    public void setUp() throws Exception {
        toolRegistry = new ToolRegistry();

    }

    @Test
    public void shouldRegisterTools() throws Exception {
        List<Tool> tools = ImmutableList.of(new TestTool());
        assertThat(toolRegistry.registerTools(tools)).usingFieldByFieldElementComparator().contains(new ToolInfo("TEST", 1, 1));
    }

    @Test
    public void shouldDecreaseAvailableCapacity() throws Exception {
        List<Tool> tools = ImmutableList.of(new TestTool());
        toolRegistry.registerTools(tools);
        toolRegistry.decreaseAvailableCapacity("TEST");
        assertThat(toolRegistry.getAvailableCapacity()).isEmpty();
    }

    @Test
    public void shouldFailToDecreaseAvailableCapacityBelowZero() throws Exception {
        List<Tool> tools = ImmutableList.of(new TestTool());
        toolRegistry.registerTools(tools);
        toolRegistry.decreaseAvailableCapacity("TEST");
        assertThat(toolRegistry.getAvailableCapacity()).isEmpty();
        assertThatThrownBy(() -> toolRegistry.decreaseAvailableCapacity("TEST")).isInstanceOf(OmakaseRuntimeException.class)
                .hasMessage("Unable to decrease the TEST tool's available capacity, capacity is already 0");
    }

    @Test
    public void shouldIncreaseAvailableCapacity() throws Exception {
        List<Tool> tools = ImmutableList.of(new TestTool());
        toolRegistry.registerTools(tools);
        toolRegistry.decreaseAvailableCapacity("TEST");
        assertThat(toolRegistry.getAvailableCapacity()).isEmpty();
        toolRegistry.increaseAvailableCapacity("TEST");
        assertThat(toolRegistry.getAvailableCapacity()).usingFieldByFieldElementComparator().contains(new ToolInfo("TEST", 1, 1));
    }

    @Test
    public void shouldFailToIncreaseAvailableCapacityAboveMax() throws Exception {
        List<Tool> tools = ImmutableList.of(new TestTool());
        toolRegistry.registerTools(tools);
        assertThatThrownBy(() -> toolRegistry.increaseAvailableCapacity("TEST")).isInstanceOf(OmakaseRuntimeException.class)
                .hasMessage("Unable to increase the TEST tool's available capacity as it will exceed the max capacity ");
    }

    private class TestTool implements Tool {
        @Override
        public void execute(Task task) {
            //no-op
        }

        @Override
        public String getName() {
            return "TEST";
        }
    }
}