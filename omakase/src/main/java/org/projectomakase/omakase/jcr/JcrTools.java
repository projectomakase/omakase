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
package org.projectomakase.omakase.jcr;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.Arrays;
import java.util.Optional;

/**
 * Extends ModeShape's JcrTools providing additional utility methods.
 * <p>
 * Intended for use via CDI injection.
 * </p>
 *
 * @author Richard Lucas
 */
public class JcrTools extends org.modeshape.jcr.api.JcrTools {

    @Inject
    Session session;

    /**
     * Returns an Optional containing the node type for the specified name if it exists otherwise empty.
     *
     * @param name
     *         the node types full name including it's name space
     * @return an Optional containing the node type for the specified name if it exists otherwise empty.
     */
    public Optional<NodeType> getNodeType(final String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "node type name is null or empty");
        return JcrThrowables.wrapJcrExceptionsWithReturn(() -> {
            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
            Optional<NodeType> optional = Optional.empty();
            if (nodeTypeManager.hasNodeType(name)) {
                NodeType nodeType = nodeTypeManager.getNodeType(name);
                optional = Optional.of(nodeType);
            }
            return optional;
        });
    }


    public ImmutableSet<PropertyDefinition> getPropertyDefinitions(final String name) {
        return getNodeType(name).map(nodeType -> ImmutableSet.copyOf(Arrays.asList(nodeType.getPropertyDefinitions()))).orElse(ImmutableSet.of());
    }
}
