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
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantDAO;
import org.projectomakase.omakase.content.rest.v1.model.VariantModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Variant Representation Converter
 *
 * @author Richard Lucas
 */
public class VariantRepresentationConverter implements RepresentationConverter<VariantModel, Variant> {

    @Inject
    VariantDAO variantDAO;

    @Override
    public VariantModel from(UriInfo uriInfo, Variant variant) {
        VariantModel variantModel = new VariantModel();
        variantModel.setId(variant.getId());
        variantModel.setName(variant.getVariantName());
        variantModel.setExternalIds(variant.getExternalIds());
        variantModel.setCreated(ZonedDateTime.ofInstant(variant.getCreated().toInstant(), ZoneId.systemDefault()));
        variantModel.setCreatedBy(variant.getCreatedBy());
        variantModel.setLastModified(ZonedDateTime.ofInstant(variant.getLastModified().toInstant(), ZoneId.systemDefault()));
        variantModel.setLastModifiedBy(variant.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getVariantResourceUrlAsString(uriInfo, variant, null)));
        builder.put("metadata", new Href(getVariantResourceUrlAsString(uriInfo, variant, "metadata")));
        builder.put("repositories", new Href(getVariantResourceUrlAsString(uriInfo, variant, "repositories")));
        builder.put("files", new Href(getVariantResourceUrlAsString(uriInfo, variant, "files")));
        variantModel.setLinks(builder.build());
        return variantModel;
    }

    @Override
    public Variant to(UriInfo uriInfo, VariantModel variantModel) {
        return new Variant(variantModel.getName(), variantModel.getExternalIds());
    }

    @Override
    public Variant update(UriInfo uriInfo, VariantModel variantModel, Variant variant) {
        variant.setVariantName(variantModel.getName());
        variant.setExternalIds(variantModel.getExternalIds());
        return variant;
    }

    private String getVariantResourceUrlAsString(UriInfo uriInfo, Variant variant, String path) {
        String assetId = variantDAO.getAssetId(variant);
        if (path == null) {
            return getResourceUriAsString(uriInfo, "assets", assetId, "variants", variant.getId());
        } else {
            return getResourceUriAsString(uriInfo, "assets", assetId, "variants", variant.getId(), path);
        }
    }
}
