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
package org.projectomakase.omakase.repository.rest.v1.converter;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.rest.v1.model.RepositoryModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;

import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Richard Lucas
 */
public class RepositoryRepresentationConverter implements RepresentationConverter<RepositoryModel, Repository> {

    @Override
    public RepositoryModel from(UriInfo uriInfo, Repository repository) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(repository.getId());
        repositoryModel.setName(repository.getRepositoryName());
        repositoryModel.setDescription(repository.getDescription());
        repositoryModel.setType(repository.getType());
        repositoryModel.setCreated(ZonedDateTime.ofInstant(repository.getCreated().toInstant(), ZoneId.systemDefault()));
        repositoryModel.setCreatedBy(repository.getCreatedBy());
        repositoryModel.setLastModified(ZonedDateTime.ofInstant(repository.getLastModified().toInstant(), ZoneId.systemDefault()));
        repositoryModel.setLastModifiedBy(repository.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getResourceUriAsString(uriInfo, "repositories", repository.getId())));
        builder.put("configuration", new Href(getResourceUriAsString(uriInfo, "repositories", repository.getId(), "configuration")));
        builder.put("template", new Href(getResourceUriAsString(uriInfo, "repositories", "templates", repository.getType())));
        repositoryModel.setLinks(builder.build());
        return repositoryModel;
    }

    @Override
    public Repository to(UriInfo uriInfo, RepositoryModel representation) {
        return new Repository(representation.getName(), representation.getDescription(), representation.getType());
    }

    @Override
    public Repository update(UriInfo uriInfo, RepositoryModel repositoryModel, Repository target) {
        return null;
    }
}
