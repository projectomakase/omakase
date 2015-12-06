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

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Represents a tail recursive call that computes a partial product and then either invokes the next call or returns a result.
 * <p>
 * {@link TailCall#invoke()} uses lazy iteration via {@link Stream#iterate(Object, UnaryOperator)} to determine if their is another call to invoke by creating an infinite stream of calls and
 * invoking
 * each one until a result is found. Lazy iteration ensures that only the current call is on the stack which prevents a stack overflow.
 * </p>
 *
 * @author Richard Lucas
 */
@FunctionalInterface
public interface TailCall<T> {

    /**
     * Applies the tail call function.
     *
     * @return the function result.
     */
    TailCall<T> apply();

    /**
     * True if the current tail call is the terminating tail call otherwise false. The default implementation always returns false, the terminating tail call should override this with true.
     *
     * @return true if complete otherwise false.
     */
    default boolean isComplete() {
        return false;
    }

    /**
     * Returns the result if available. The default implementation throws a {@link UnsupportedOperationException}. The terminating tail call should override this with the actual result of the
     * recursion.
     *
     * @return the result if available.
     */
    default T result() {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Repeatedly iterates through the pending tail call recursions until it reaches the end of the recursion.
     *
     * @return return the final result of the recursion.
     */
    default T invoke() {
        return Stream.iterate(this, TailCall::apply).filter(TailCall::isComplete).findFirst().get().result();
    }
}
