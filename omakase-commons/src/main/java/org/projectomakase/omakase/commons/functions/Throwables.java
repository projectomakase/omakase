/*
 * #%L
 * omakase-commons
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
package org.projectomakase.omakase.commons.functions;

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;

/**
 * Invokes a function wrapping any checked exceptions thrown by the function in an unchecked exception.
 *
 * @author Richard Lucas
 */
public final class Throwables {

    private Throwables() {
        // hides the implicit public constructor
    }

    /**
     * A function that wraps an exception in another exception.
     *
     * @param <E>
     *         the type of the exception that is used to wrap the original exception.
     */
    @FunctionalInterface
    public interface ExceptionWrapper<E> {

        /**
         * Wraps the original exception in the new exception.
         *
         * @param e
         *         the original exception
         * @return the new exception
         */
        E wrap(Exception e);
    }

    /**
     * Represents a function that returns a result.
     *
     * @param <R>
     *         the type of the result of the function
     */
    @FunctionalInterface
    public interface ReturnableInstance<R> {
        /**
         * Applies the function.
         *
         * @return the function's result.
         * @throws Exception
         *         thrown if an error occurs.
         */
        R apply() throws Exception;
    }


    /**
     * Represents a function that does no return a result.
     */
    @FunctionalInterface
    public interface VoidInstance {
        /**
         * Applies the function.
         *
         * @throws Exception
         *         thrown if an error occurs.
         */
        void apply() throws Exception;
    }

    /**
     * Executes a {@link Throwables.VoidInstance} wrapping any exceptions in a OmakaseRuntimeException.
     *
     * @param voidInstance
     *         a function that does not return a result
     * @throws OmakaseRuntimeException
     *         if the function throws an exception.
     */
    public static void voidInstance(VoidInstance voidInstance) {
        voidInstance(voidInstance, OmakaseRuntimeException::new);
    }

    /**
     * Executes a {@link Throwables.VoidInstance} wrapping any excpetions with the specified exception wrapper.
     *
     * @param voidInstance
     *         a function that does not return a result
     * @param wrapper
     *         an exception wrapper
     * @param <E>
     *         the type of the exception that should be used to wrap the original exception.
     * @throws E
     *         if the function throws an exception
     */
    public static <E extends Throwable> void voidInstance(VoidInstance voidInstance, ExceptionWrapper<E> wrapper) throws E {
        try {
            voidInstance.apply();
        } catch (Exception e) {
            throw wrapper.wrap(e);
        }
    }

    /**
     * Executes a {@link Throwables.ReturnableInstance} wrapping any exceptions in a OmakaseRuntimeException.
     *
     * @param returnableInstance
     *         a function that returns a result
     * @param <R>
     *         the type of the result of the function
     * @return the result of the function.
     * @throws OmakaseRuntimeException
     *         if the function throws an exception.
     */
    public static <R> R returnableInstance(ReturnableInstance<R> returnableInstance) throws OmakaseRuntimeException {
        return returnableInstance(returnableInstance, OmakaseRuntimeException::new);
    }

    /**
     * Executes a {@link Throwables.ReturnableInstance} wrapping any exceptions with the specified exception wrapper.
     *
     * @param returnableInstance
     *         a function that returns a result
     * @param wrapper
     *         the exception wrapper
     * @param <R>
     *         the type of the result of the function
     * @param <E>
     *         the type of the exception that should be used to wrap the original exception.
     * @return the result of the function.
     * @throws E
     *         if the function throws an exception.
     */
    public static <R, E extends Throwable> R returnableInstance(ReturnableInstance<R> returnableInstance, ExceptionWrapper<E> wrapper) throws E {
        try {
            return returnableInstance.apply();
        } catch (Exception e) {
            throw wrapper.wrap(e);
        }
    }


}
