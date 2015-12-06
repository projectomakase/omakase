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
package org.projectomakase.omakase.content.rest.v1.converter;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.rest.v1.model.AssetModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Asset Representation Converter
 *
 * @author Richard Lucas
 */
public class AssetRepresentationConverter implements RepresentationConverter<AssetModel, Asset> {

    @Override
    public AssetModel from(UriInfo uriInfo, Asset asset) {
        AssetModel assetModel = new AssetModel();
        assetModel.setId(asset.getId());
        assetModel.setName(asset.getAssetName());
        assetModel.setExternalIds(asset.getExternalIds());
        assetModel.setCreated(ZonedDateTime.ofInstant(asset.getCreated().toInstant(), ZoneId.systemDefault()));
        assetModel.setCreatedBy(asset.getCreatedBy());
        assetModel.setLastModified(ZonedDateTime.ofInstant(asset.getLastModified().toInstant(), ZoneId.systemDefault()));
        assetModel.setLastModifiedBy(asset.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getAssetResourceUrlAsString(uriInfo, asset, null)));
        builder.put("related", new Href(getAssetResourceUrlAsString(uriInfo, asset, "related")));
        builder.put("parent", new Href(getAssetResourceUrlAsString(uriInfo, asset, "parent")));
        builder.put("children", new Href(getAssetResourceUrlAsString(uriInfo, asset, "children")));
        builder.put("variants", new Href(getAssetResourceUrlAsString(uriInfo, asset, "variants")));
        builder.put("metadata", new Href(getAssetResourceUrlAsString(uriInfo, asset, "metadata")));
        assetModel.setLinks(builder.build());
        return assetModel;
    }

    @Override
    public Asset to(UriInfo uriInfo, AssetModel assetModel) {
        return new Asset(assetModel.getName(), assetModel.getExternalIds());
    }

    @Override
    public Asset update(UriInfo uriInfo, AssetModel assetModel, Asset asset) {
        asset.setAssetName(assetModel.getName());
        asset.setExternalIds(assetModel.getExternalIds());
        return asset;
    }

    private String getAssetResourceUrlAsString(UriInfo uriInfo, Asset asset, String path) {
        if (path == null) {
            return getResourceUriAsString(uriInfo, "assets", asset.getId());
        } else {
            return getResourceUriAsString(uriInfo, "assets", asset.getId(), path);
        }
    }
}
