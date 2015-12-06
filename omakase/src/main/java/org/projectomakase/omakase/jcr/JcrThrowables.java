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

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.exceptions.NotFoundException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlException;

/**
 * Provides methods for executing blocks of code that throw javax.jcr.RepositoryException and wrapping the exceptions in unchecked application exceptions.
 *
 * @author Richard Lucas
 */
public final class JcrThrowables {

    private JcrThrowables() {
        // hides the implicit public constructor
    }

    /**
     * Represents a JCR action that accepts a block of code and returns a result T
     *
     * @param <T>
     *         the return type
     */
    @FunctionalInterface
    public static interface JcrRepositoryAction<T> {

        /**
         * Performs this operation and returns the results
         *
         * @return the result of the operation
         * @throws RepositoryException thrown if a JCR error occurs
         */
        T accepts() throws RepositoryException;
    }

    /**
     * Represents a JCR action that accepts a block of code and returns no result
     */
    @FunctionalInterface
    public static interface JcrVoidRepositoryAction {

        /**
         * Performs the operation
         *
         * @throws RepositoryException thrown if a JCR error occurs
         */
        void run() throws RepositoryException;
    }

    /**
     * Accepts a JcrRepositoryAction and either returns the result on success or wraps the RepositoryException in an unchecked exception and re-throws it if an error occurs
     *
     * @param action
     *         the action to accept
     * @param <T>
     *         the return type
     * @param <E>
     *         the exception type
     * @return the result of the action
     * @throws E thrown if a JCR error occurs
     */
    public static <T, E extends Throwable> T wrapJcrExceptionsWithReturn(JcrRepositoryAction<T> action) throws E {
        try {
            return action.accepts();
        } catch (AccessControlException e) {
            throw new NotAuthorizedException(e);
        } catch (PathNotFoundException e) {
            throw new NotFoundException(e);
        } catch (ConstraintViolationException e) {
            throw new InvalidPropertyException(e);
        } catch (RepositoryException e) {
            throw new OmakaseRuntimeException(e);
        }
    }

    /**
     * Accepts a JcrVoidRepositoryAction and wraps the RepositoryException in an unchecked exception and re-throws it if an error occurs
     *
     * @param action
     *         the action to accept
     * @param <E>
     *         the exception type
     * @throws E thrown if a JCR error occurs
     */
    public static <E extends Throwable> void wrapJcrExceptions(JcrVoidRepositoryAction action) throws E {
        try {
            action.run();
        } catch (AccessControlException e) {
            throw new NotAuthorizedException(e);
        } catch (PathNotFoundException e) {
            throw new NotFoundException(e);
        } catch (ConstraintViolationException e) {
            throw new InvalidPropertyException(e);
        } catch (RepositoryException e) {
            throw new OmakaseRuntimeException(e);
        }
    }
}
