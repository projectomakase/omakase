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
package org.projectomakase.omakase.jcr.query;

/**
 * Default {@link SQL2QueryBuilder} implementation. All of the search conditions are be applied to the specified node type.
 *
 * @author Richard Lucas
 */
public class DefaultSQL2QueryBuilder extends AbstractSQL2QueryBuilder {

    private static final String SELECT_CLAUSE = "SELECT node.* FROM [%s] AS node WHERE ISDESCENDANTNODE('%s')";

    private final String nodeType;
    private final String parentNodePath;

    public DefaultSQL2QueryBuilder(String nodeType, String parentNodePath) {
        this.nodeType = nodeType;
        this.parentNodePath = parentNodePath;
    }

    @Override
    public String build() {
        return build(String.format(SELECT_CLAUSE, nodeType, parentNodePath));
    }


}
