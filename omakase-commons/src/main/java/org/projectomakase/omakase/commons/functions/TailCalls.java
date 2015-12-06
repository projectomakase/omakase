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

/**
 * Convenience class used to invoke the next tail call, or return the result if recursion is complete.
 *
 * @author Richard Lucas
 */
public final class TailCalls {

    private TailCalls() {
        // hides the implicit public constructor
    }

    /**
     * Invokes the the next recursive tail call.
     *
     * @param nextCall
     *         the next recursive tail call to invoke.
     * @param <T>
     *         the return type of the tail call.
     * @return the result of invoking the next tail call.
     */
    public static <T> TailCall<T> call(final TailCall<T> nextCall) {
        return nextCall;
    }

    /**
     * Returns the final result of the recursive tail calls.
     *
     * @param value
     *         the result to return.
     * @param <T>
     *         the return type of the tail calls.
     * @return the final result of the recursive tail calls.
     */
    public static <T> TailCall<T> done(final T value) {
        return new DoneTailCall<>(value);
    }

    private static class DoneTailCall<T> implements TailCall<T> {

        private final T value;

        public DoneTailCall(T value) {
            this.value = value;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public T result() {
            return value;
        }

        @Override
        public TailCall<T> apply() {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
