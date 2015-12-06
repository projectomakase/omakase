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
package org.projectomakase.omakase.rest.converter;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Interface for representation converter implementations responsible for converting the REST Model to/from the Source Model
 *
 * @param <T> the REST object's type
 * @param <S> the source object's type
 * @author Richard Lucas
 */
public interface RepresentationConverter<T, S> {

    /**
     * Creates a new REST object from the Source object
     *
     * @param uriInfo
     *         the uriInfo context
     * @param source
     *         the Source object
     * @return the REST object
     */
    T from(UriInfo uriInfo, S source);

    /**
     * Creates a collection of REST objects from a collection of source objects
     *
     * @param uriInfo
     *         the uriInfo context
     * @param sources
     *         a collection of source objects
     * @return a collection of REST objects from a collection of source objects
     */
    default Collection<T> from(UriInfo uriInfo, Collection<S> sources) {
        return sources.stream().map(in -> from(uriInfo, in)).collect(Collectors.toList());
    }

    /**
     * Creates a new Source object from the REST object
     *
     * @param uriInfo
     *         the uriInfo context
     * @param representation
     *         the REST object
     * @return the Source object
     */
    S to(UriInfo uriInfo, T representation);

    /**
     * Updates an existing Source object with the contents of the REST object
     *
     * @param uriInfo
     *         the uriInfo context
     * @param representation
     *         the REST object
     * @param target
     *         the Source object
     * @return the updated source object
     */
    S update(UriInfo uriInfo, T representation, S target);

    /**
     * Creates a collection of source objects from a collection of REST objects
     *
     * @param uriInfo
     *         the uriInfo context
     * @param representations
     *         a collection of REST objects
     * @return a collection of source objects from a collection of REST objects
     */
    default Collection<S> to(UriInfo uriInfo, Collection<T> representations) {
        return representations.stream().map(in -> to(uriInfo, in)).collect(Collectors.toList());
    }

    /**
     * Returns a resource URI built using the base URI in the uriInfo context and paths as a string
     *
     * @param uriInfo
     *         the uriInfo context
     * @param path
     *         optional paths to append to the resource uri
     * @return a resource URI built using the base URI in the uriInfo context and paths as a string
     */
    default String getResourceUriAsString(UriInfo uriInfo, String... path) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        Arrays.asList(path).forEach(p -> uriBuilder.path(p));
        return uriBuilder.build().toString();
    }
}
