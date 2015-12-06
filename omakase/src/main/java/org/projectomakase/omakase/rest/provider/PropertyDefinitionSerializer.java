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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.CaseFormat;
import org.projectomakase.omakase.commons.functions.Throwables;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.IOException;
import java.util.Arrays;

public class PropertyDefinitionSerializer extends JsonSerializer<PropertyDefinition> {

    @Override
    public void serialize(PropertyDefinition propertyDefinition, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("name", getPropertyNameWithOutNamespace(propertyDefinition.getName()));
        generator.writeStringField("type", PropertyType.nameFromValue(propertyDefinition.getRequiredType()));
        generator.writeBooleanField("required", propertyDefinition.isMandatory());
        generator.writeBooleanField("multi_values", propertyDefinition.isMultiple());
        writeConstraints(generator, propertyDefinition.getValueConstraints());
        writeValues(generator,  propertyDefinition.getDefaultValues());
        generator.writeEndObject();
    }

    @Override
    public Class<PropertyDefinition> handledType() {
        return PropertyDefinition.class;
    }



    private static String getPropertyNameWithOutNamespace(String propertyName) {
        String[] split = propertyName.split(":");
        String name;
        if (split.length > 1) {
            name =  split[1];
        } else {
            name = propertyName;
        }
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    private static void writeConstraints(JsonGenerator generator, String[] values) throws IOException {
        if (values != null && values.length > 0) {
            generator.writeStartArray();
            Arrays.stream(values).forEach(valueConstraint -> Throwables.voidInstance(() -> generator.writeString(valueConstraint)));
            generator.writeEndArray();
        }
    }

    private void writeValues(JsonGenerator generator, Value[] values) throws IOException {
        if (values != null && values.length > 0) {
            generator.writeStartArray();
            Arrays.stream(values).forEach(value -> Throwables.voidInstance(() -> generator.writeString(value.getString())));
            generator.writeEndArray();
        }
    }

}
