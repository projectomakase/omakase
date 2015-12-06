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
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.VariantRepositoryDAO;
import org.projectomakase.omakase.content.rest.v1.model.RepositoryModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Repository Representation Converter
 *
 * @author Richard Lucas
 */
public class RepositoryRepresentationConverter implements RepresentationConverter<RepositoryModel, VariantRepository> {

    @Inject
    VariantRepositoryDAO variantRepositoryDAO;

    @Override
    public RepositoryModel from(UriInfo uriInfo, VariantRepository repository) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(repository.getId());
        repositoryModel.setName(repository.getRepositoryName());
        repositoryModel.setCreated(ZonedDateTime.ofInstant(repository.getCreated().toInstant(), ZoneId.systemDefault()));
        repositoryModel.setCreatedBy(repository.getCreatedBy());
        repositoryModel.setLastModified(ZonedDateTime.ofInstant(repository.getLastModified().toInstant(), ZoneId.systemDefault()));
        repositoryModel.setLastModifiedBy(repository.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getRepositoryResourceUrlAsString(uriInfo, repository, null)));
        builder.put("tier", new Href(getRepositoryResourceUrlAsString(uriInfo, repository, "tier")));
        builder.put("repository", new Href(getResourceUriAsString(uriInfo, "repositories", repository.getId())));
        repositoryModel.setLinks(builder.build());
        return repositoryModel;
    }

    @Override
    public VariantRepository to(UriInfo uriInfo, RepositoryModel repository) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariantRepository update(UriInfo uriInfo, RepositoryModel representation, VariantRepository target) {
        throw new UnsupportedOperationException();
    }

    private String getRepositoryResourceUrlAsString(UriInfo uriInfo, VariantRepository variantRepository, String path) {
        String assetId = variantRepositoryDAO.getAssetId(variantRepository);
        String variantId = variantRepositoryDAO.getVariantId(variantRepository);
        if (path == null) {
            return getResourceUriAsString(uriInfo, "assets", assetId, "variants", variantId, "repositories", variantRepository.getId());
        } else {
            return getResourceUriAsString(uriInfo, "assets", assetId, "variants", variantId, "repositories", variantRepository.getId(), path);
        }
    }
}
