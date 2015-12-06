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
package org.projectomakase.omakase.content;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.jcr.AbstractJcrDAO;
import org.projectomakase.omakase.jcr.JcrThrowables;
import org.jcrom.Jcrom;
import org.jcrom.util.NodeFilter;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Variant DAO.
 *
 * @author Richard Lucas
 */
public class VariantDAO extends AbstractJcrDAO<Variant> {

    @Inject
    public VariantDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    public String getAssetId(Variant variant) {
        return JcrThrowables.wrapJcrExceptionsWithReturn(() -> getNode(variant.getNodePath()).getParent().getName());
    }

    /**
     * Finds the variant for the specified variant id.
     *
     * @param variantId
     *         the variant id.
     * @return an Optional containing the variant or empty if no variant was found.
     */
    public Optional<Variant> findVariantById(@NotNull String variantId) {
        String sql = "SELECT variant.* " +
                "FROM [omakase:variant] AS variant " +
                "WHERE variant.[jcr:name] = '" + variantId + "'";
        return super.findBySql(sql, new NodeFilter(NodeFilter.INCLUDE_ALL, NodeFilter.DEPTH_INFINITE)).stream().findFirst();
    }
}

