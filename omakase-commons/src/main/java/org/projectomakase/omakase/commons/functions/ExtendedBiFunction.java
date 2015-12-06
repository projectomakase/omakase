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

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides additional functionality not provided by the standard {@link BiFunction} implementation.
 *
 * @author Richard Lucas
 */
@FunctionalInterface
public interface ExtendedBiFunction<T, U, R>
        extends BiFunction<T, U, R> {

    default Function<U, R> curry1(T t) {
        return u -> apply(t, u);
    }

    default Function<T, R> curry2(U u) {
        return t -> apply(t, u);
    }

    default <V> ExtendedBiFunction<V, U, R> compose1(Function<? super V, ? extends T> before) {
        return (v, u) -> apply(before.apply(v), u);
    }

    default <V> ExtendedBiFunction<T, V, R> compose2(Function<? super V, ? extends U> before) {
        return (t, v) -> apply(t, before.apply(v));
    }
}
