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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Richard Lucas
 */
@ApplicationScoped
public class ToolRegistry {

    private static final Logger LOGGER = Logger.getLogger(ToolRegistry.class);

    private final Cache<String, ToolInfo> cache = CacheBuilder.newBuilder().concurrencyLevel(1).build();

    public List<ToolInfo> registerTools(List<Tool> tools) {
        List<ToolInfo> toolInfos = tools.stream().map(ToolRegistry::getToolInfo).collect(ImmutableListCollector.toImmutableList());
        toolInfos.forEach(toolInfo -> cache.put(toolInfo.getName(), toolInfo));
        return toolInfos;
    }

    public void decreaseAvailableCapacity(String toolName) {
        ToolInfo toolInfo = cache.getIfPresent(toolName);
        if (toolInfo != null) {
            int available = toolInfo.getAvailableCapacity() - 1;
            if (available < 0) {
                throw new OmakaseRuntimeException("Unable to decrease the " + toolName + " tool's available capacity, capacity is already 0");
            }
            ToolInfo updatedToolInfo = new ToolInfo(toolName, toolInfo.getMaxCapacity(), available);
            cache.put(toolName, updatedToolInfo);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updated " + updatedToolInfo);
            }
        }
    }

    public void increaseAvailableCapacity(String toolName) {
        ToolInfo toolInfo = cache.getIfPresent(toolName);
        if (toolInfo != null) {
            int capacity = toolInfo.getAvailableCapacity() + 1;
            if (capacity > toolInfo.getMaxCapacity()) {
                throw new OmakaseRuntimeException("Unable to increase the " + toolName + " tool's available capacity as it will exceed the max capacity ");
            }
            ToolInfo updatedToolInfo = new ToolInfo(toolName, toolInfo.getMaxCapacity(), capacity);
            cache.put(toolName, updatedToolInfo);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updated " + updatedToolInfo);
            }
        }
    }

    public Set<ToolInfo> getAvailableCapacity() {
        return cache.asMap().entrySet().stream().map(Map.Entry::getValue).filter(toolInfo -> toolInfo.getAvailableCapacity() > 0).collect(ImmutableSetCollector.toImmutableSet());
    }

    private static ToolInfo getToolInfo(Tool tool) {
        int capacity = getMaxCapacity(tool.getName());
        return new ToolInfo(tool.getName(), capacity, capacity);
    }

    private static int getMaxCapacity(String toolName) {
        return Integer.parseInt(ConfigResolver.getPropertyValue(toolName.toLowerCase() + ".max.capacity", "1"));
    }
}
