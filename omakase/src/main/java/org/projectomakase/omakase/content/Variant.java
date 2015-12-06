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
package org.projectomakase.omakase.content;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Variant
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = "omakase:variant")
public class Variant extends JcrEntity {

    public static final String VARIANT_NAME = "omakase:name";
    public static final String EXTERNAL_IDS = "omakase:externalIds";

    private static final String TYPE = "omakase:type";

    @JcrProperty(name = VARIANT_NAME)
    private String variantName;
    @JcrProperty(name = EXTERNAL_IDS)
    private List<String> externalIds = new ArrayList<>();
    @JcrProperty(name = TYPE)
    private VariantType type = VariantType.FILE;

    public Variant() {
        //required by Jcrom
    }

    public Variant(String variantName, List<String> externalIds) {
        this.variantName = variantName;
        this.externalIds = externalIds;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public List<String> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(List<String> externalIds) {
        this.externalIds = externalIds;
    }

    public VariantType getType() {
        return type;
    }

    public void setType(VariantType type) {
        this.type = type;
    }
}
