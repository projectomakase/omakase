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
package org.projectomakase.omakase.event;

/**
 * Notifies observers that a Variant is being deleted from a repository.
 *
 * @author Richard Lucas
 */
public class DeleteVariantFromRepository {

    private final String variantId;
    private final String repositoryId;

    public DeleteVariantFromRepository(String variantId, String repositoryId) {
        this.variantId = variantId;
        this.repositoryId = repositoryId;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }
}
