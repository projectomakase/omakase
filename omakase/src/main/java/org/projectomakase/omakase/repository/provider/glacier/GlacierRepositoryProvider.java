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
package org.projectomakase.omakase.repository.provider.glacier;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.aws.glacier.GlacierClient;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.commons.hash.Hashes;
import org.projectomakase.omakase.jcr.JcrTools;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.projectomakase.omakase.repository.api.RepositoryException;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.spi.MultipartUpload;
import org.projectomakase.omakase.repository.spi.RemoteDelete;
import org.projectomakase.omakase.repository.spi.RepositoryProvider;
import org.projectomakase.omakase.repository.spi.Restorable;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;
import java.util.Optional;

/**
 * Glacier Repository Provider implementation.
 *
 * @author Richard Lucas
 */
public class GlacierRepositoryProvider implements RepositoryProvider<GlacierRepositoryConfiguration>, Restorable, RemoteDelete, MultipartUpload {

    private static final String TYPE = "GLACIER";

    private static final Logger LOGGER = Logger.getLogger(GlacierRepositoryProvider.class);

    @Inject
    JcrTools jcrTools;
    @Inject
    IdGenerator idGenerator;
    @Inject
    @Omakase
    GlacierClient glacierClient;
    @Inject
    @ConfigProperty(name = "omakase.glacier.upload.part.size")
    long partSize;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Class<GlacierRepositoryConfiguration> getConfigurationType() {
        return GlacierRepositoryConfiguration.class;
    }

    @Override
    public ImmutableSet<PropertyDefinition> getConfigurationTemplate() {
        return jcrTools.getPropertyDefinitions(GlacierRepositoryConfiguration.MIXIN);
    }

    @Override
    public void validateConfiguration(GlacierRepositoryConfiguration repositoryConfiguration) {
        try {
            Regions.fromName(repositoryConfiguration.getRegion().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new RepositoryConfigurationException("invalid AWS region " + repositoryConfiguration.getRegion(), e);
        }

        if (repositoryConfiguration.getAwsSecretKey().contains("/")) {
            throw new RepositoryConfigurationException("AWS secret keys that contain a '/' are not supported, please use a different key");
        }
    }

    @Override
    public URI getRepositoryUri(Repository repository) {
        GlacierRepositoryConfiguration configuration = getConfiguration(repository);
        String host = "glacier." + configuration.getRegion().toLowerCase() + ".amazonaws.com";
        String path = "/-/vaults/" + configuration.getVault();
        return Throwables.returnableInstance(() -> new URI("glacier", configuration.getAwsAccessKey() + ":" + configuration.getAwsSecretKey(), host, -1, path, null, null));
    }

    @Override
    public Optional<URI> getNewFileUri() {
        return Optional.empty();
    }

    @Override
    public void restore(Repository repository, RepositoryFile repositoryFile, String correlationId) {
        try {
            GlacierRepositoryConfiguration configuration = getConfiguration(repository);
            glacierClient.restore(new BasicAWSCredentials(configuration.getAwsAccessKey(), configuration.getAwsSecretKey()), configuration.getRegion(), configuration.getVault(),
                                  repositoryFile.getRelativePath(), configuration.getSnsTopicArn(), correlationId);
        } catch (OmakaseRuntimeException e) {
            String message = "Failed to restore file " + repositoryFile.getId() + " in repository " + repository.getId();
            LOGGER.error(message, e);
            throw new RepositoryException(message, e);
        }
    }

    @Override
    public void delete(Repository repository, RepositoryFile repositoryFile) {
        try {
            GlacierRepositoryConfiguration configuration = getConfiguration(repository);
            glacierClient.deleteArchive(new BasicAWSCredentials(configuration.getAwsAccessKey(), configuration.getAwsSecretKey()), configuration.getRegion(), configuration.getVault(),
                                        repositoryFile.getRelativePath());
        } catch (OmakaseRuntimeException e) {
            String message = "Failed to delete file " + repositoryFile.getId() + " in repository " + repository.getId();
            LOGGER.error(message, e);
            throw new RepositoryException(message, e);
        }
    }

    @Override
    public boolean requiresMultipartUpload() {
        return true;
    }

    @Override
    public MultipartUploadInfo getMultipartUploadInfo() {
        return new MultipartUploadInfo(partSize, ImmutableList.of(Hashes.SHA256, Hashes.TREE_HASH));
    }

    private static GlacierRepositoryConfiguration getConfiguration(Repository repository) {
        return (GlacierRepositoryConfiguration) repository.getRepositoryConfiguration();
    }
}
