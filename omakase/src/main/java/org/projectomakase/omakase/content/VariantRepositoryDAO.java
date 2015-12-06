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

import javax.inject.Inject;
import javax.jcr.Session;

/**
 * Variant Repository DAO.
 *
 * @author Richard Lucas
 */
public class VariantRepositoryDAO extends AbstractJcrDAO<VariantRepository> {

    @Inject
    public VariantRepositoryDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    public String getAssetId(VariantRepository variantRepository) {
        return JcrThrowables.wrapJcrExceptionsWithReturn(() -> getNode(variantRepository.getNodePath()).getParent().getParent().getName());
    }

    public String getVariantId(VariantRepository variantRepository) {
        return JcrThrowables.wrapJcrExceptionsWithReturn(() -> getNode(variantRepository.getNodePath()).getParent().getName());
    }
}

