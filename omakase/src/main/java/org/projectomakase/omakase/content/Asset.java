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
 * Represents an Asset
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = "omakase:asset")
public class Asset extends JcrEntity {

    public static final String ASSET_NAME = "omakase:name";
    public static final String EXTERNAL_IDS = "omakase:externalIds";

    @JcrProperty(name = ASSET_NAME)
    private String assetName;
    @JcrProperty(name = EXTERNAL_IDS)
    private List<String> externalIds = new ArrayList<>();

    public Asset() {
        //required by Jcrom
    }

    public Asset(String assetName, List<String> externalIds) {
        this.assetName = assetName;
        this.externalIds = externalIds;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public List<String> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(List<String> externalIds) {
        this.externalIds = externalIds;
    }

}
