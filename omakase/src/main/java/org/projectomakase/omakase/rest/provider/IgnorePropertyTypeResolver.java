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
package org.projectomakase.omakase.rest.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.util.Collection;

/**
 * Custom Jackson {@link com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder} that creates a type serializer that
 * does NOT serialize the type property.
 * <p>
 * This is used in cases where we need to include type information in the incoming JSON to correctly deserialize a polymorphic type but
 * do not want to include the type property in the outgoing JSON.
 * </p>
 *
 * @author Richard Lucas
 */
public class IgnorePropertyTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

    public IgnorePropertyTypeResolver() {
        super(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    /**
     * Performs a no-op when serializing type information
     */
    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
        return null;
    }
}
