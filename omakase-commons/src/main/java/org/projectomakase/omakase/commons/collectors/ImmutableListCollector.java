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
package org.projectomakase.omakase.commons.collectors;

import com.google.common.collect.ImmutableList;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Custom {@link Collector} that accumulates the input elements into a Guava {@link ImmutableList}.
 *
 * @param <E>
 *         the type of elements in the list
 * @author Richard Lucas
 */
public class ImmutableListCollector<E> implements Collector<E, ImmutableList.Builder<E>, ImmutableList<E>> {

    public static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
        return new ImmutableListCollector<>();
    }

    @Override
    public Supplier<ImmutableList.Builder<E>> supplier() {
        return ImmutableList::builder;
    }

    @Override
    public BiConsumer<ImmutableList.Builder<E>, E> accumulator() {
        return ImmutableList.Builder::add;
    }

    @Override
    public BinaryOperator<ImmutableList.Builder<E>> combiner() {
        return (left, right) -> {
            left.addAll(right.build());
            return left;
        };
    }

    @Override
    public Function<ImmutableList.Builder<E>, ImmutableList<E>> finisher() {
        return ImmutableList.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

}
