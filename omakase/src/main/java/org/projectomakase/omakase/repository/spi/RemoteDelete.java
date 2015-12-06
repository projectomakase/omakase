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
package org.projectomakase.omakase.repository.spi;

import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;

/**
 * Implemented by repository provider implementations that support remote delete e.g. the content stored in the repository can be removed via an API call and does not require direct access to the
 * storage.
 *
 * @author Richard Lucas
 */
@FunctionalInterface
public interface RemoteDelete {

    /**
     * Deletes the repository file from the repository.
     * <p>
     * This is a synchronous call and returns the result of the delete immediately.
     * </p>
     *
     * @param repository
     *         the repository
     * @param repositoryFile
     *         the repositoryFile
     */
    void delete(Repository repository, RepositoryFile repositoryFile);
}
