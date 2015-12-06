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
package org.projectomakase.omakase.content.rest.v1;

import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantRepository;
import org.projectomakase.omakase.content.rest.v1.converter.AssetQuerySearchConverter;
import org.projectomakase.omakase.content.rest.v1.converter.FileQuerySearchConverter;
import org.projectomakase.omakase.content.rest.v1.converter.RepositoryQuerySearchConverter;
import org.projectomakase.omakase.content.rest.v1.converter.VariantQuerySearchConverter;
import org.projectomakase.omakase.content.rest.v1.model.AssetModel;
import org.projectomakase.omakase.content.rest.v1.model.FileModel;
import org.projectomakase.omakase.content.rest.v1.model.RepositoryModel;
import org.projectomakase.omakase.content.rest.v1.model.VariantModel;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

/**
 * Content Management JAX-RS Resource implementation. Exposes REST API operations for Assets, Variants, Variant Repositories and File resources.
 * <p>
 * The different resources are handled by their own JAX-RS sub-resource implementations. The content management resource is responsible for initiating the sub resources and mapping them to the
 * correct
 * path.
 *
 * @author Richard Lucas
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class ContentResource {

    @Inject
    ContentManager contentManager;
    @Inject
    JobManager jobManager;
    @Inject
    RepresentationConverter<AssetModel, Asset> assetRepresentationConverter;
    @Inject
    AssetQuerySearchConverter assetQuerySearchConverter;
    @Inject
    RepresentationConverter<VariantModel, Variant> variantRepresentationConverter;
    @Inject
    VariantQuerySearchConverter variantQuerySearchConverter;
    @Inject
    RepresentationConverter<RepositoryModel, VariantRepository> repositoryRepresentationConverter;
    @Inject
    RepositoryQuerySearchConverter repositoryQuerySearchConverter;
    @Inject
    RepresentationConverter<FileModel, VariantFile> fileRepresentationConverter;
    @Inject
    FileQuerySearchConverter fileQuerySearchConverter;
    @Context
    UriInfo uriInfo;
    @Context
    Request request;

    @Path("/assets")
    public AssetsResource getAssetsResource() {
        return new AssetsResource(contentManager, assetRepresentationConverter, assetQuerySearchConverter, uriInfo);
    }

    @Path("/assets/{assetId}")
    public AssetResource getAssetResource() {
        return new AssetResource(contentManager, assetRepresentationConverter, uriInfo, request);
    }

    @Path("/assets/{assetId}/variants")
    public VariantsResource getVariantsResource() {
        return new VariantsResource(contentManager, variantRepresentationConverter, variantQuerySearchConverter, uriInfo);
    }

    @Path("/assets/{assetId}/variants/{variantId}")
    public VariantResource getVariantResource() {
        return new VariantResource(contentManager, jobManager, variantRepresentationConverter, uriInfo, request);
    }

    @Path("/assets/{assetId}/variants/{variantId}/repositories")
    public RepositoriesResource getRepositoriesResource() {
        return new RepositoriesResource(contentManager, repositoryRepresentationConverter, repositoryQuerySearchConverter, uriInfo);
    }

    @Path("/assets/{assetId}/variants/{variantId}/repositories/{repositoryId}")
    public RepositoryResource getRepositoryResource() {
        return new RepositoryResource(contentManager, repositoryRepresentationConverter, uriInfo, request);
    }

    @Path("/assets/{assetId}/variants/{variantId}/repositories/{repositoryId}/status")
    public RepositoryStatusResource getRepositoryStatusResource() {
        return new RepositoryStatusResource(contentManager);
    }

    @Path("/assets/{assetId}/variants/{variantId}/files")
    public FilesResource getFilesResource() {
        return new FilesResource(contentManager, fileRepresentationConverter, fileQuerySearchConverter, uriInfo);
    }

    @Path("/assets/{assetId}/variants/{variantId}/files/{fileId}")
    public FileResource getFileResource() {
        return new FileResource(contentManager, fileRepresentationConverter, uriInfo, request);
    }
}
