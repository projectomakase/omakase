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
 * Implemented by repository provider implementations that may require files being restored to online prior to being able to access them.
 * <p>
 * Repository Provider implementations that need to support restore semantics should implement this interface. Implementing this interface ensures that a restore will be performed, if required, when
 * exporting or replicating content out of the repository.
 * </p>
 *
 * @author Richard Lucas
 */
@FunctionalInterface
public interface Restorable {

    /**
     * Requests that the specified file be restored to online in the specified repository.
     * <p>
     * This may take several hours to complete.
     * </p>
     *
     * @param repository
     *         the repository
     * @param repositoryFile
     *         the file to restore
     * @param correlationId
     *         a correlationId
     */
    void restore(Repository repository, RepositoryFile repositoryFile, String correlationId);
}
