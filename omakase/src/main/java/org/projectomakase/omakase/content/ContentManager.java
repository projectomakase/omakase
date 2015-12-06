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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.event.DeleteAsset;
import org.projectomakase.omakase.event.DeleteVariant;
import org.projectomakase.omakase.event.DeleteVariantFromRepository;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.DeleteVariantJobConfiguration;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchException;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.logging.Logger;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Java facade for managing content
 *
 * @author Richard Lucas
 */
@Stateless
public class ContentManager {

    private static final Logger LOGGER = Logger.getLogger(ContentManager.class);

    @Inject
    @OrganizationNodePath()
    String organizationNodePath;
    @Inject
    AssetDAO assetDAO;
    @Inject
    VariantDAO variantDAO;
    @Inject
    VariantRepositoryDAO variantRepositoryDAO;
    @Inject
    VariantFileDAO variantFileDAO;
    @Inject
    IdGenerator idGenerator;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    JobManager jobManager;
    @Inject
    Event<DeleteAsset> deleteAssetEvent;
    @Inject
    Event<DeleteVariant> deleteVariantEvent;
    @Inject
    Event<DeleteVariantFromRepository> deleteVariantFromRepositoryEvent;

    /**
     * Creates a new asset.
     *
     * @param asset
     *         the new asset.
     * @return the newly created asset.
     * @throws InvalidPropertyException
     *         if asset contains invalid properties.
     */
    public Asset createAsset(@NotNull final Asset asset) {
        String id = idGenerator.getId();
        asset.setId(id);
        return assetDAO.distributedCreate(getAssetPath(Optional.empty()), asset, id);
    }

    /**
     * Returns all of the assets that match the given search.
     *
     * @param search
     *         the search
     * @return all of the assets that match the given search.
     * @throws SearchException
     *         if the search fails
     */
    public SearchResult<Asset> findAssets(@NotNull final Search search) {
        return assetDAO.findNodes(getAssetPath(Optional.empty()), search, "omakase:asset");
    }

    /**
     * Returns the asset for the given assetId if it exists otherwise returns an empty Optional.
     *
     * @param assetId
     *         the asset id.
     * @return the asset for the given asset assetId if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the asset.
     */
    public Optional<Asset> getAsset(@NotNull final String assetId) {
        return Optional.ofNullable(assetDAO.get(getAssetPath(Optional.of(assetId))));
    }

    /**
     * Updates the given asset
     *
     * @param asset
     *         the asset being updated.
     * @return the updated asset.
     * @throws NotAuthorizedException
     *         if the user does not have access to the asset.
     * @throws NotFoundException
     *         if an asset does not exist for the given asset id.
     * @throws InvalidPropertyException
     *         if asset contains invalid properties.
     */
    public Asset updateAsset(@NotNull final Asset asset) {
        return assetDAO.update(asset);
    }

    /**
     * Deletes the asset for the given asset id.
     *
     * @param assetId
     *         the asset id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the asset.
     * @throws NotFoundException
     *         if an asset does not exist for the given asset id.
     * @throws NotUpdateableException
     *         if the asset can not be deleted.
     */
    public void deleteAsset(@NotNull final String assetId) {
        deleteAsset(assetId, false);
    }

    /**
     * Deletes the asset for the given asset id.
     *
     * @param assetId
     *         the asset id.
     * @param force
     *         if true the asset will be deleted even if it has jobs or variants associated to it. This may result in jobs failing and orphaned files.
     * @throws NotAuthorizedException
     *         if the user does not have access to the asset.
     * @throws NotFoundException
     *         if an asset does not exist for the given asset id.
     * @throws NotUpdateableException
     *         if the asset can not be deleted.
     */
    public void deleteAsset(@NotNull final String assetId, boolean force) {
        if (!force) {
            if (findVariants(assetId, new AssetSearchBuilder().onlyCount(true).build()).getTotalRecords() > 0) {
                throw new NotUpdateableException("One or more variants are associated to asset " + assetId);
            }
            deleteAssetEvent.fire(new DeleteAsset(assetId));
        }
        assetDAO.remove(getAssetPath(Optional.of(assetId)));
    }

    /**
     * Creates a new variant under the specified asset.
     *
     * @param assetId
     *         the asset id
     * @param variant
     *         the new variant.
     * @return the newly created variant.
     */

    public Variant createVariant(@NotNull final String assetId, @NotNull final Variant variant) {
        variant.setId(idGenerator.getId());
        return variantDAO.create(getVariantPath(assetId, Optional.empty()), variant);
    }

    /**
     * Returns all of the variants for the specified asset that match the given search.
     *
     * @param assetId
     *         the asset id
     * @param search
     *         the search
     * @return all of the variants for the specified asset that match the given search.
     */
    public SearchResult<Variant> findVariants(@NotNull final String assetId, @NotNull final Search search) {
        return variantDAO.findNodes(getVariantPath(assetId, Optional.empty()), search, "omakase:variant");
    }

    /**
     * Returns the variant for the given asset and variant ids if it exists otherwise returns an empty Optional.
     * <p>
     * This is the most efficient way to retrieve a variant and should be used if the asset id is known.
     * </p>
     *
     * @param assetId
     *         the assetId.
     * @param variantId
     *         the variant id.
     * @return the variant for the given asset and variant ids if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the variant.
     */
    public Optional<Variant> getVariant(@NotNull final String assetId, @NotNull final String variantId) {
        return Optional.ofNullable(variantDAO.get(getVariantPath(assetId, Optional.of(variantId))));
    }

    /**
     * Returns the variant for the given variant id if it exists otherwise returns an empty Optional.
     * <p>
     * This should only be used if the asset id is not known, otherwise use {@link ContentManager#getVariant(String, String)}.
     * </p>
     *
     * @param variantId
     *         the variant id.
     * @return the variant for the given variant id if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the variant.
     */
    public Optional<Variant> getVariant(final String variantId) {
        if (Strings.isNullOrEmpty(variantId)) {
            return Optional.empty();
        } else {
            return variantDAO.findVariantById(variantId);
        }

    }

    /**
     * Updates the variant for the given asset id and variant.
     *
     * @param variant
     *         the variant.
     * @return the updated variant.
     * @throws NotAuthorizedException
     *         if the user does not have access to the variant.
     * @throws NotFoundException
     *         if the variant does not exist for the given variant and asset ids.
     */
    public Variant updateVariant(@NotNull final Variant variant) {
        return variantDAO.update(variant);
    }

    /**
     * Deletes the variant for the given asset and variant id.
     * <p>
     * If the variant has files associated with it the variant is removed and a job is created to delete the files asynchronously. A job id is returned to track the progress. If the variant
     * does not have any files associated with it, the variant is removed and no further action is taken.
     * </p>
     *
     * @param assetId
     *         the asset id.
     * @param variantId
     *         the variant id.
     * @return an Optional containing the job id of the job created to delete the variant, or empty of no job was required to delete the variant.
     * @throws NotAuthorizedException
     *         if the user does not have access to the variant.
     * @throws NotFoundException
     *         if the variant does not exist for the given variant and asset ids.
     * @throws NotUpdateableException
     *         if the variant can not be deleted.
     */
    public Optional<String> deleteVariant(@NotNull final String assetId, @NotNull final String variantId) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        Optional<String> jobId = Optional.empty();
        deleteVariantEvent.fire(new DeleteVariant(variantId));
        List<VariantRepository> variantRepositories = findVariantRepositories(assetId, variantId, new VariantRepositorySearchBuilder().build()).getRecords();
        if (!variantRepositories.isEmpty()) {
            Job deleteJob = jobManager.createJob(Job.Builder.build(job -> {
                job.setJobName("Delete variant " + variantId);
                job.setJobType(JobType.DELETE);
                job.setExternalIds(ImmutableList.of(variantId));
                job.setStatus(JobStatus.QUEUED);
                job.setSystem(true);
                job.setJobConfiguration(
                        new DeleteVariantJobConfiguration(variantId, variantRepositories.stream().map(VariantRepository::getId).collect(ImmutableListCollector.toImmutableList())));
            }));
            jobId = Optional.of(deleteJob.getId());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created job " + deleteJob.getId() + " to delete variant " + variantId);
            }
        }
        variantDAO.remove(variant.getNodePath());
        return jobId;
    }

    /**
     * Deletes the variant for the given asset and variant id from the specified repository.
     * <p>
     * A job is created to delete the files asynchronously. A job id is returned to track the progress.
     * </p>
     *
     * @param assetId
     *         the asset id
     * @param variantId
     *         the variant id
     * @param repositoryId
     *         the repository id
     * @return the job id of the job created to delete the variant from the repository.
     * @throws NotAuthorizedException
     *         if the user does not have access to the variant.
     * @throws NotFoundException
     *         if the variant does not exist for the given variant and asset id, or the variant is not in the specified repository.
     * @throws NotUpdateableException
     *         if the variant can not be deleted.
     */
    public String deleteVariantFromRepository(@NotNull final String assetId, @NotNull final String variantId, @NotNull String repositoryId) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        VariantRepository variantRepository = getVariantRepository(assetId, variantId, repositoryId).orElseThrow(NotFoundException::new);

        deleteVariantFromRepositoryEvent.fire(new DeleteVariantFromRepository(variantId, repositoryId));

        // we look up the repo count prior to doing a remove in order to work around a modeshape query issue
        long variantRepoCount = findVariantRepositories(assetId, variantId, new VariantRepositorySearchBuilder().onlyCount(true).build()).getTotalRecords();

        variantRepositoryDAO.remove(variantRepository.getNodePath());

        if (variantRepoCount - 1 == 0) {
            variantFileDAO.removeAllFilesFromVariant(variant.getNodePath());
        }

        Job deleteJob = jobManager.createJob(Job.Builder.build(job -> {
            job.setJobName("Delete variant " + variantId + " from repository " + repositoryId);
            job.setJobType(JobType.DELETE);
            job.setExternalIds(ImmutableList.of(variantId));
            job.setStatus(JobStatus.QUEUED);
            job.setSystem(true);
            job.setJobConfiguration(new DeleteVariantJobConfiguration(variantId, ImmutableList.of(repositoryId)));
        }));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created job " + deleteJob.getId() + " to delete variant " + variantId + " from repository " + repositoryId);
        }

        return deleteJob.getId();
    }

    /**
     * Associates the specified variant to the specified repository. This should be called after a successful ingest of the variant into the repository.
     *
     * @param variantId
     *         the variant id
     * @param variantRepositories
     *         a list of {@link VariantRepository} objects that represents the repositories the variant was ingested into.
     */
    public void associateVariantToRepositories(@NotNull final String variantId, @NotNull final List<VariantRepository> variantRepositories) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        variantRepositories.forEach(variantRepository -> {
            if (!getVariantRepository(variantId, variantRepository.getId()).isPresent()) {
                variantRepositoryDAO.create(variant.getNodePath(), variantRepository);
            }
        });
    }

    /**
     * Returns all of the repositories that a variant is stored in that match the given search.
     *
     * @param assetId
     *         the asset id
     * @param variantId
     *         the variant id
     * @param search
     *         the search
     * @return all of the repositories that a variant is stored in that match the given search.
     */
    public SearchResult<VariantRepository> findVariantRepositories(@NotNull final String assetId, @NotNull final String variantId, @NotNull final Search search) {
        return variantRepositoryDAO.findNodes(getVariantRepositoryPath(assetId, variantId, Optional.empty()), search, "omakase:variantRepository");
    }

    /**
     * Returns all of the repositories that a variant is stored in that match the given search.
     * <p>
     * This should only be used if the asset id is not known, otherwise use {@link ContentManager#findVariantRepositories(String, String, Search)}.
     * </p>
     *
     * @param variantId
     *         the variant id
     * @param search
     *         the search
     * @return all of the repositories that a variant is stored in that match the given search.
     */
    public SearchResult<VariantRepository> findVariantRepositories(@NotNull final String variantId, @NotNull final Search search) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        String assetId = variantDAO.getAssetId(variant);
        return variantRepositoryDAO.findNodes(getVariantRepositoryPath(assetId, variantId, Optional.empty()), search, "omakase:variantRepository");
    }

    /**
     * Returns the repository for the given asset, variant and repository if it exists otherwise returns an empty Optional.
     *
     * @param assetId
     *         the asset id
     * @param variantId
     *         the variant id
     * @param repositoryId
     *         the repository id
     * @return the repository for the given asset, variant and repository if it exists otherwise returns an empty Optional.
     */
    public Optional<VariantRepository> getVariantRepository(@NotNull final String assetId, @NotNull final String variantId, @NotNull final String repositoryId) {
        return Optional.ofNullable(variantRepositoryDAO.get(getVariantRepositoryPath(assetId, variantId, Optional.of(repositoryId))));
    }

    /**
     * Returns the repository for the given variant and repository if it exists otherwise returns an empty Optional.
     * <p>
     * This should only be used if the asset id is not known, otherwise use {@link ContentManager#getVariantRepository(String, String, String)}.
     * </p>
     *
     * @param variantId
     *         the variant id
     * @param repositoryId
     *         the repository id
     * @return the repository for the given asset, variant and repository if it exists otherwise returns an empty Optional.
     */
    public Optional<VariantRepository> getVariantRepository(@NotNull final String variantId, @NotNull final String repositoryId) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        String assetId = variantDAO.getAssetId(variant);
        return Optional.ofNullable(variantRepositoryDAO.get(getVariantRepositoryPath(assetId, variantId, Optional.of(repositoryId))));
    }

    /**
     * Creates a new variant file for the given variant.
     *
     * @param variantId
     *         the variant id
     * @param variantFile
     *         the variant file
     * @return the newly created variant file.
     */
    public VariantFile createVariantFile(String variantId, VariantFile variantFile) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        String id = idGenerator.getId();
        variantFile.setId(id);
        return variantFileDAO.distributedCreate(variant.getNodePath(), variantFile, id);
    }

    /**
     * Returns all of the variant files that match the given search.
     * <p>
     * This is the most efficient way to find variant repositories and should be used if the asset id is known.
     * </p>
     *
     * @param assetId
     *         the asset id
     * @param variantId
     *         the variant id
     * @param search
     *         the search
     * @return all of the variant files that match the given search.
     */
    public SearchResult<VariantFile> findVariantFiles(@NotNull final String assetId, @NotNull final String variantId, @NotNull final Search search) {
        return variantFileDAO.findNodes(getVariantFilePath(assetId, variantId, Optional.empty()), search, "omakase:variantFile");
    }

    /**
     * Returns all of the variant files that match the given search.
     * <p>
     * This should only be used if the asset id is not known, otherwise use {@link ContentManager#findVariantFiles(String, String,
     * Search)}.
     * </p>
     *
     * @param variantId
     *         the variant id
     * @param search
     *         the search
     * @return all of the variant files that match the given search.
     */
    public SearchResult<VariantFile> findVariantFiles(@NotNull final String variantId, @NotNull final Search search) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        return variantFileDAO.findNodes(variant.getNodePath(), search, "omakase:variantFile");
    }

    /**
     * Returns the file for the given asset, variant and file id if it exists otherwise returns an empty Optional.
     *
     * @param assetId
     *         the asset id
     * @param variantId
     *         the variant id
     * @param fileId
     *         the file id
     * @return the file for the given asset, variant and file id if it exists otherwise returns an empty Optional.
     */
    public Optional<VariantFile> getVariantFile(@NotNull final String assetId, @NotNull final String variantId, @NotNull final String fileId) {
        return Optional.ofNullable(variantFileDAO.get(getVariantFilePath(assetId, variantId, Optional.of(fileId))));
    }

    /**
     * Returns the file for the given variant and file id if it exists otherwise returns an empty Optional.
     * <p>
     * This should only be used if the asset id is not known, otherwise use {@link #getVariantFile(String, String, String)}.
     * </p>
     *
     * @param variantId
     *         the variant id
     * @param fileId
     *         the file id
     * @return the file for the given asset, variant and file id if it exists otherwise returns an empty Optional.
     */
    public Optional<VariantFile> getVariantFile(@NotNull final String variantId, @NotNull final String fileId) {
        Variant variant = getVariantOrThrowNotFound(variantId);
        String assetId = variantDAO.getAssetId(variant);
        return Optional.ofNullable(variantFileDAO.get(getVariantFilePath(assetId, variantId, Optional.of(fileId))));
    }

    /**
     * Creates a new child variant file for the given parent variant file.
     *
     * @param variantId
     *         the variant id
     * @param parent
     *         the parent variant file
     * @param child
     *         the child variant file.
     * @return the newly created variant file.
     * @throws IllegalArgumentException
     *         if the parent variant file does not have the type {@link VariantFileType#VIRTUAL}
     */
    public VariantFile createChildVariantFile(String variantId, VariantFile parent, VariantFile child) {

        getVariantOrThrowNotFound(variantId);

        if (!VariantFileType.VIRTUAL.equals(parent.getType())) {
            throw new IllegalArgumentException("The parent variant file must be of type VIRTUAL");
        }

        child.setId(idGenerator.getId());
        child.setType(VariantFileType.CHILD);
        return variantFileDAO.create(parent.getNodePath(), child);
    }

    /**
     * Returns all of the child {@link VariantFile} instances for the give parent variant file.
     *
     * @param assetId
     *         the asset id
     * @param variantId
     *         the variant id
     * @param parent
     *         the parent variant file
     * @return all of the child {@link VariantFile} instances for the give parent variant file.
     */
    public Set<VariantFile> getChildVariantFiles(@NotNull final String assetId, @NotNull final String variantId, @NotNull final String parent) {
        VariantFile virtualVariantFile = getVariantFile(assetId, variantId, parent).orElseThrow(() -> new NotFoundException("Variant File " + parent + " does not exist"));
        return variantFileDAO.getChildVariantFiles(virtualVariantFile.getNodePath());
    }

    /**
     * Returns all of the child {@link VariantFile} instances for the give parent variant file.
     * <p>
     * This should only be used if the asset id is not known, otherwise use {@link #getChildVariantFiles(String, String, String)}.
     * </p>
     *
     * @param variantId
     *         the variant id
     * @param parent
     *         the parent variant file
     * @return all of the child {@link VariantFile} instances for the give parent variant file.
     */
    public Set<VariantFile> getChildVariantFiles(@NotNull final String variantId, @NotNull final String parent) {
        Variant variant = getVariant(variantId).orElseThrow(NotFoundException::new);
        String assetId = variantDAO.getAssetId(variant);
        return getChildVariantFiles(assetId, variantId, parent);
    }

    private Variant getVariantOrThrowNotFound(@NotNull String variantId) {
        return getVariant(variantId).orElseThrow(() -> new NotFoundException("Variant " + variantId + " does not exist"));
    }

    private String getAssetPath(Optional<String> assetId) {
        String path = organizationNodePath + "/assets";
        if (assetId.isPresent()) {
            String id = assetId.get();
            path = assetDAO.getDistributedNodePath(path, id);
        }
        return path;
    }

    private String getVariantPath(String assetId, Optional<String> variantId) {
        String path = getAssetPath(Optional.of(assetId));
        if (variantId.isPresent()) {
            path += "/" + variantId.get();
        }
        return path;
    }

    private String getVariantRepositoryPath(String assetId, String variantId, Optional<String> repositoryId) {
        String path = getVariantPath(assetId, Optional.of(variantId));
        if (repositoryId.isPresent()) {
            path += "/" + repositoryId.get();
        }
        return path;
    }

    private String getVariantFilePath(String assetId, String variantId, Optional<String> fileId) {
        String path = getVariantPath(assetId, Optional.of(variantId));
        if (fileId.isPresent()) {
            String id = fileId.get();
            path = variantFileDAO.getDistributedNodePath(path, id);
        }
        return path;
    }
}
