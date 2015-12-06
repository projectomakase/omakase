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
package org.projectomakase.omakase.repository;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.event.DeleteVariantFromRepository;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.jcr.Template;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.projectomakase.omakase.repository.api.RepositoryException;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.spi.MultipartUpload;
import org.projectomakase.omakase.repository.spi.RemoteDelete;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.projectomakase.omakase.repository.spi.RepositoryProvider;
import org.projectomakase.omakase.repository.spi.Restorable;
import org.projectomakase.omakase.search.DefaultSearchBuilder;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.search.SearchException;
import org.reflections.Reflections;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Java Facade for managing repositories.
 *
 * @author Richard Lucas
 */
@Stateless
public class RepositoryManager {

    private static final Logger LOGGER = Logger.getLogger(RepositoryManager.class);

    @Inject
    @OrganizationNodePath("repositories")
    String repositoriesNodePath;
    @Inject
    RepositoryDAO repositoryDAO;
    @Inject
    RepositoryFileDAO repositoryFileDAO;
    @Inject
    RepositoryProviderResolver repositoryProviderResolver;
    @Inject
    IdGenerator idGenerator;
    @Inject
    Reflections reflections;
    @Inject
    Event<DeleteVariantFromRepository> deleteVariantFromRepositoryEvent;

    /**
     * Creates a new repository that is backed by a repository provider implementation.
     *
     * @param repository
     *         the repository to create.
     * @return a new repository that is backed by a repository provider implementation.
     */
    public Repository createRepository(@NotNull Repository repository) {
        try {
            repositoryProviderResolver.getRepositoryProvider(repository.getType());
        } catch (OmakaseRuntimeException e) {
            throw new RepositoryConfigurationException("Invalid repository type " + repository.getType(), e);
        }
        repository.setId(idGenerator.getId());
        return repositoryDAO.create(getRepositoryPath(Optional.empty()), repository);
    }

    /**
     * Returns all of the jobs that match the given search constraints.
     *
     * @param search
     *         search constraints
     * @return all of the jobs that match the given search constraints.
     * @throws SearchException
     *         if there is an error executing the search.
     */
    public SearchResult<Repository> findRepositories(@NotNull final Search search) {
        return repositoryDAO.findNodes(getRepositoryPath(Optional.empty()), search, "omakase:repository");
    }

    /**
     * Returns all of the repositories that a given variant is stored in.
     *
     * @param variantId
     *         the variant id
     * @return all of the repositories that a given variant is stored in.
     */
    public ImmutableSet<Repository> getRepositoriesForVariant(final String variantId) {
        return ImmutableSet.copyOf(repositoryDAO.findByVariantId(variantId));
    }

    /**
     * Returns the repository for the given repository id if it exists otherwise returns an empty Optional.
     *
     * @param repositoryId
     *         the repository id.
     * @return the repository for the given repository id if it exists otherwise returns an empty Optional.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     */
    public Optional<Repository> getRepository(@NotNull final String repositoryId) {
        if (Strings.isNullOrEmpty(repositoryId)) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(repositoryDAO.get(getRepositoryPath(Optional.of(repositoryId))));
        }
    }

    /**
     * Deletes the repository for the given repository id.
     *
     * @param repositoryId
     *         the repository id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if an repository does not exist for the given repository name.
     * @throws NotUpdateableException
     *         if the repository has files
     */
    public void deleteRepository(@NotNull final String repositoryId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));
        SearchResult<RepositoryFile> searchResult = repositoryFileDAO.findNodes(repository.getNodePath(), new DefaultSearchBuilder().onlyCount(true).build(), "omakase:repositoryFile");
        if (searchResult.getTotalRecords() > 0) {
            throw new NotUpdateableException("Unable to delete repository that contains files");
        }
        repositoryDAO.remove(getRepositoryPath(Optional.of(repositoryId)));
    }

    /**
     * Deletes the repository for the given repository name.
     *
     * @param repositoryId
     *         the repository id.
     * @param force
     *         deletes the repository even if it has repository files.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if an repository does not exist for the given repository name.
     */
    public void deleteRepository(@NotNull final String repositoryId, boolean force) {
        if (force) {
            String nodePath = getRepositoryPath(Optional.of(repositoryId));
            repositoryDAO.remove(nodePath);
        } else {
            deleteRepository(repositoryId);
        }
    }

    /**
     * Returns the {@link RepositoryConfiguration} type
     *
     * @param repositoryId
     *         the repository id
     * @param <T>
     *         the {@link RepositoryConfiguration} type
     * @return the {@link RepositoryConfiguration} type
     */
    public <T extends RepositoryConfiguration> Class<T> getRepositoryConfigurationType(@NotNull final String repositoryId) {
        RepositoryProvider<T> repositoryProvider = getRepositoryProvider(repositoryId);
        return repositoryProvider.getConfigurationType();
    }

    /**
     * Returns a set of supported repository templates;
     *
     * @return a set of supported repository templates;
     */
    @SuppressWarnings("unchecked")
    public ImmutableSet<Template> getRepositoryTemplates() {
        return repositoryProviderResolver.getRepositoryProviders().stream().map(provider -> new Template(provider.getType(), provider.getConfigurationTemplate())).collect(
                ImmutableSetCollector.toImmutableSet());
    }

    /**
     * Returns the specified repository template
     *
     * @param templateName
     *         the template name
     * @return the specified repository template
     * @throws NotFoundException
     *         if the template does not exist
     */
    @SuppressWarnings("unchecked")
    public Template getRepositoryTemplate(@NotNull String templateName) {
        try {
            RepositoryProvider provider = repositoryProviderResolver.getRepositoryProvider(templateName);
            return new Template(templateName, provider.getConfigurationTemplate());
        } catch (OmakaseRuntimeException e) {
            throw new NotFoundException("template " + templateName + "not found", e);
        }
    }

    /**
     * Updates the repository configuration for the specified repository
     *
     * @param repositoryId
     *         the repository id.
     * @param repositoryConfiguration
     *         the repository configuration.
     * @return the repository configuration.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if an repository does not exist for the given repository name.
     */
    @SuppressWarnings("unchecked")
    public RepositoryConfiguration updateRepositoryConfiguration(@NotNull final String repositoryId, @NotNull final RepositoryConfiguration repositoryConfiguration) {
        Repository currentRepository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));
        RepositoryProvider<RepositoryConfiguration> repositoryProvider = getRepositoryProvider(currentRepository);
        repositoryProvider.validateConfiguration(repositoryConfiguration);
        currentRepository.setRepositoryConfiguration(repositoryConfiguration);
        Repository updatedRepository = repositoryDAO.update(currentRepository);
        return updatedRepository.getRepositoryConfiguration();
    }

    /**
     * Returns the absolute URI for the root of the repository.
     *
     * @param repositoryId
     *         the repository id.
     * @return the absolute URI for the root of the repository.
     */
    public URI getRepositoryUri(@NotNull final String repositoryId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));
        return getRepositoryProvider(repository).getRepositoryUri(repository);
    }

    /**
     * Returns true if the repository supports restore otherwise false.
     * <p>
     * Repositories that support restore may require the files to be restored to online prior to the file being accessible.
     * </p>
     *
     * @param repositoryId
     *         the repository id
     * @return true if the repository supports restore otherwise false.
     */
    public boolean doesRepositoryRequireRestore(@NotNull final String repositoryId) {
        return getRepositoryProvider(repositoryId) instanceof Restorable;
    }

    /**
     * Requests that the specified file be restored to online in the specified repository.
     * <p>
     * This may take several hours to complete.
     * </p>
     *
     * @param repositoryId
     *         the repository id
     * @param repositoryFile
     *         the file to restore
     * @param correlationId
     *         a correlation id
     * @throws RepositoryException
     *         if the repository does not support restore or an error occurs getting the restore status.
     */
    public void restore(@NotNull final String repositoryId, @NotNull final RepositoryFile repositoryFile, @NotNull String correlationId) {
        if (!doesRepositoryRequireRestore(repositoryId)) {
            throw new RepositoryException("Repository " + repositoryId + " does not support restore");
        }
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));
        ((Restorable) getRepositoryProvider(repository)).restore(repository, repositoryFile, correlationId);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Requested restore for file " + repositoryFile.getId() + " into repository " + repositoryId);
        }
    }

    /**
     * Returns true if the repository requires a multipart upload, otherwise false.
     *
     * @param repositoryId
     *         the repository id
     * @return true if the repository requires a multipart upload, otherwise false.
     */
    public boolean doesRepositoryRequireMultipartUpload(@NotNull final String repositoryId) {
        RepositoryProvider repositoryProvider = getRepositoryProvider(repositoryId);
        return repositoryProvider instanceof MultipartUpload && ((MultipartUpload) repositoryProvider).requiresMultipartUpload();
    }

    /**
     * Returns the multipart upload information for the specified repository.
     *
     * @param repositoryId
     *         the repository id
     * @return the multipart upload information for the specified repository.
     */
    public MultipartUploadInfo getMultipartUploadInfoForRepository(@NotNull final String repositoryId) {
        RepositoryProvider repositoryProvider = getRepositoryProvider(repositoryId);
        if (!(repositoryProvider instanceof MultipartUpload)) {
            throw new RepositoryException("Repository " + repositoryId + " does not support multipart upload");
        }
        return ((MultipartUpload) repositoryProvider).getMultipartUploadInfo();
    }

    /**
     * Creates a new {@link RepositoryFile} in the specified repository for the variant.
     * <p>
     * The relative path of the file within the repository is automatically assigned to the repository file.
     * </p>
     *
     * @param variantId
     *         the variant id.
     * @param repositoryId
     *         the repository id.
     * @return the newly created repository file.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if the repository does not exist for the given repository name.
     */
    public RepositoryFile createRepositoryFile(final String variantId, @NotNull final String repositoryId) {
        return createRepositoryFile(variantId, repositoryId, null);
    }

    /**
     * Creates a new {@link RepositoryFile} in the specified repository for the variant.
     *
     * @param variantId
     *         the variant id.
     * @param repositoryId
     *         the repository id.
     * @param variantFileId
     *         the variant file id associated with the repository file.
     * @return the newly created repository file.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if the repository does not exist for the given repository name.
     */
    public RepositoryFile createRepositoryFile(final String variantId, @NotNull final String repositoryId, String variantFileId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));
        if (repository.getRepositoryConfiguration() == null) {
            throw new RepositoryConfigurationException("Repository " + repositoryId + " is not configured");
        }
        RepositoryFile repositoryFile = getRepositoryProvider(repository).getNewFileUri().map(path -> new RepositoryFile(path.getPath(), variantId)).orElse(new RepositoryFile(variantId));
        String id = idGenerator.getId();
        repositoryFile.setId(id);
        if (variantFileId != null) {
            repositoryFile.setVariantFileId(variantFileId);
        }
        return repositoryFileDAO.distributedCreate(repository.getNodePath(), repositoryFile, id);
    }

    /**
     * Returns the repository file for the given repository and repository file id.
     *
     * @param repositoryId
     *         the repository id.
     * @param repositoryFileId
     *         the repository file id.
     * @return the repository file for the given repository and repository file id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if the repository does not exist.
     */
    public Optional<RepositoryFile> getRepositoryFile(@NotNull final String repositoryId, final String repositoryFileId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));

        if (repository.getRepositoryConfiguration() == null) {
            throw new RepositoryConfigurationException("Repository " + repositoryId + " is not configured");
        }

        return Optional.ofNullable(repositoryFileDAO.get(getRepositoryFilePath(repositoryId, repositoryFileId)));
    }

    /**
     * Returns the repository file for the given repository and variant file id.
     *
     * @param repositoryId
     *         the repository id.
     * @param variantFileId
     *         the variant file id.
     * @return the repository file for the given repository and variant file id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if the repository does not exist.
     */
    public Optional<RepositoryFile> getRepositoryFileForVariantFile(@NotNull final String repositoryId, final String variantFileId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));

        if (repository.getRepositoryConfiguration() == null) {
            throw new RepositoryConfigurationException("Repository " + repositoryId + " is not configured");
        }

        return repositoryFileDAO.findByVariantFileIdAndRepositoryId(variantFileId, repositoryId);
    }

    /**
     * Returns a list of repository files for the given repository and variant.
     *
     * @param repositoryId
     *         the repository id
     * @param variantId
     *         the variant id
     * @return a list of repository files for the given repository and variant.
     */
    public ImmutableList<RepositoryFile> getRepositoryFilesForVariant(@NotNull final String repositoryId, final String variantId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));

        if (repository.getRepositoryConfiguration() == null) {
            throw new RepositoryConfigurationException("Repository " + repositoryId + " is not configured");
        }
        return ImmutableList.copyOf(repositoryFileDAO.findByVariantIdAndRepositoryId(variantId, repositoryId));
    }

    /**
     * Updates the repository file.
     *
     * @param repositoryFile
     *         the repository file
     * @return the updated repository file.
     */
    public RepositoryFile updateRepositoryFile(RepositoryFile repositoryFile) {
        return repositoryFileDAO.update(repositoryFile);
    }

    /**
     * Deletes the {@link RepositoryFile} from the repository.
     *
     * @param repositoryId
     *         the repository id.
     * @param repositoryFileId
     *         the repository file id.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if the repository or repository file does not exist.
     */
    public void deleteRepositoryFile(@NotNull final String repositoryId, final String repositoryFileId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));

        if (repository.getRepositoryConfiguration() == null) {
            throw new RepositoryConfigurationException("Repository " + repositoryId + " is not configured");
        }

        repositoryFileDAO.remove(Optional.ofNullable(repositoryFileDAO.get(getRepositoryFilePath(repositoryId, repositoryFileId))).map(RepositoryFile::getNodePath)
                                         .orElseThrow(() -> new NotFoundException("Repository file " + repositoryFileId + " does not exist in repository " + repositoryId)));
    }

    /**
     * Deletes all {@link RepositoryFile} instances from the repository for the specified variant.
     *
     * @param repositoryId
     *         the repository id.
     * @param variantId
     *         the id of the variant the files belong to.
     * @throws NotAuthorizedException
     *         if the user does not have access to the repository.
     * @throws NotFoundException
     *         if the repository or repository file does not exist.
     */
    public void deleteVariantFromRepository(@NotNull final String repositoryId, final String variantId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));

        if (repository.getRepositoryConfiguration() == null) {
            throw new RepositoryConfigurationException("Repository " + repositoryId + " is not configured");
        }
        List<RepositoryFile> repositoryFiles = repositoryFileDAO.findByVariantIdAndRepositoryId(variantId, repositoryId);
        if (repositoryFiles.isEmpty()) {
            throw new NotFoundException("Variant " + variantId + " does not exist in repository " + repositoryId);
        }
        deleteVariantFromRepositoryEvent.fire(new DeleteVariantFromRepository(variantId, repositoryId));

        boolean supportsRemoteDelete = doesRepositorySupportRemoteDelete(repositoryId);

        repositoryFiles.forEach(repositoryFile -> {
            repositoryDAO.remove(repositoryFile.getNodePath());
            // remove physical files, this is non-transactional and will not be rolled back
            if (supportsRemoteDelete) {
                RemoteDelete remoteDelete = (RemoteDelete) getRepositoryProvider(repository);
                remoteDelete.delete(repository, repositoryFile);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleted repository file " + repositoryFile.getId() + " from repository " + repositoryId);
            }
        });

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleted variant " + variantId + " from repository " + repositoryId);
        }
    }

    /**
     * Returns true if the repository supports remote delete otherwise false.
     *
     * @param repositoryId
     *         the repository id
     * @return true if the repository supports remote delete otherwise false.
     */
    public boolean doesRepositorySupportRemoteDelete(@NotNull final String repositoryId) {
        return getRepositoryProvider(repositoryId) instanceof RemoteDelete;
    }

    /**
     * Returns true if the repository supports remote delete otherwise false.
     *
     * @param repository
     *         the repository
     * @return true if the repository supports remote delete otherwise false.
     */
    public boolean doesRepositorySupportRemoteDelete(@NotNull final Repository repository) {
        return getRepositoryProvider(repository) instanceof RemoteDelete;
    }

    private <T extends RepositoryConfiguration> RepositoryProvider<T> getRepositoryProvider(@NotNull final String repositoryId) {
        Repository repository = getRepository(repositoryId).orElseThrow(() -> new NotFoundException("Repository " + repositoryId + " does not exist"));
        return getRepositoryProvider(repository);
    }

    @SuppressWarnings("unchecked")
    private <T extends RepositoryConfiguration> RepositoryProvider<T> getRepositoryProvider(@NotNull final Repository repository) {
        return (RepositoryProvider<T>) repositoryProviderResolver.getRepositoryProvider(repository.getType());
    }

    private String getRepositoryPath(Optional<String> repositoryId) {
        String path = repositoriesNodePath;
        if (repositoryId.isPresent()) {
            path += "/" + repositoryId.get();
        }
        return path;
    }

    private String getRepositoryFilePath(String repositoryId, String repositoryFileId) {
        return repositoryFileDAO.getDistributedNodePath(repositoriesNodePath + "/" + repositoryId, repositoryFileId);
    }
}
