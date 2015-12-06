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

import org.projectomakase.omakase.jcr.modeshape.OmakaseCredentials;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * JCR Session Producer
 *
 * @author Richard Lucas
 */
public class SessionProducer {

    private static final Logger LOGGER = Logger.getLogger(SessionProducer.class);

    @Resource(mappedName = "java:/jcr/omakase-repo")
    Repository repository;

    /**
     * Produces a request scoped JCR Session using the authenticated callers credentials when invoked via CDI injection.
     * <p>
     * This method should not be called directly, instead the sessions should be obtained as follows:
     * </p>
     * <code>
     * {@literal @}Inject Session session
     * </code>
     *
     * @return a request scoped JCR Session
     * @throws RepositoryException
     *         if an error occurs getting the session
     */
    @RequestScoped
    @Produces
    public Session getSession() throws RepositoryException {
        LOGGER.trace("Creating new session...");
        return repository.login(new OmakaseCredentials());
    }

    /**
     * Automatically calls logout on the request scoped JCR Session (assuming it was obtained via CDI injection) when the request ends.
     * <p>
     * This method should not be called directly.
     * </p>
     *
     * @param session
     *         the current JCR Session
     */
    public void logoutSession(@Disposes final Session session) {
        LOGGER.trace("Closing session...");
        session.logout();
    }
}
