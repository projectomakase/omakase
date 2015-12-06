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
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileDAO;
import org.projectomakase.omakase.content.rest.v1.model.FileModel;
import org.projectomakase.omakase.content.rest.v1.model.HashModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * File Representation Converter
 *
 * @author Richard Lucas
 */
public class FileRepresentationConverter implements RepresentationConverter<FileModel, VariantFile> {

    @Inject
    VariantFileDAO variantFileDAO;

    @Override
    public FileModel from(UriInfo uriInfo, VariantFile file) {
        FileModel fileModel = new FileModel();
        fileModel.setId(file.getId());
        fileModel.setName(file.getVariantFilename());
        if (file.getSize() != null) {
            fileModel.setSize(file.getSize());
        }
        if (file.getOriginalFilename() != null) {
            fileModel.setOriginalFileName(file.getOriginalFilename());
        }
        fileModel.setHashes(file.getHashes().stream().map(hash -> new HashModel(hash.getHash(), hash.getHashAlgorithm())).collect(ImmutableListCollector.toImmutableList()));
        fileModel.setCreated(ZonedDateTime.ofInstant(file.getCreated().toInstant(), ZoneId.systemDefault()));
        ImmutableMap.Builder<String, Href> linkMapBuilder = ImmutableMap.builder();
        linkMapBuilder.put("self", new Href(getFileResourceUrlAsString(uriInfo, file, null)));
        fileModel.setLinks(linkMapBuilder.build());
        return fileModel;
    }

    @Override
    public VariantFile to(UriInfo uriInfo, FileModel fileModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariantFile update(UriInfo uriInfo, FileModel representation, VariantFile target) {
        throw new UnsupportedOperationException();
    }

    private String getFileResourceUrlAsString(UriInfo uriInfo, VariantFile variantFile, String path) {
        String assetId = variantFileDAO.getAssetId(variantFile);
        String variantId = variantFileDAO.getVariantId(variantFile);
        if (path == null) {
            return getResourceUriAsString(uriInfo, "assets", assetId, "variants", variantId, "files", variantFile.getId());
        } else {
            return getResourceUriAsString(uriInfo, "assets", assetId, "variants", variantId, "files", variantFile.getId(), path);
        }
    }
}
