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
package org.projectomakase.omakase.repository.provider.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;
import org.projectomakase.omakase.commons.aws.s3.S3Client;
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
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;
import java.util.Optional;

/**
 * S3 Repository Provider implementation.
 *
 * @author Richard Lucas
 */
public class S3RepositoryProvider implements RepositoryProvider<S3RepositoryConfiguration>, RemoteDelete, MultipartUpload {

    private static final Logger LOGGER = Logger.getLogger(S3RepositoryProvider.class);

    private static final String TYPE = "S3";

    @Inject
    JcrTools jcrTools;
    @Inject
    IdGenerator idGenerator;
    @Inject
    @Omakase
    S3Client s3Client;

    @Inject
    @ConfigProperty(name = "omakase.s3.upload.part.size")
    long partSize;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Class<S3RepositoryConfiguration> getConfigurationType() {
        return S3RepositoryConfiguration.class;
    }

    @Override
    public ImmutableSet<PropertyDefinition> getConfigurationTemplate() {
        return jcrTools.getPropertyDefinitions(S3RepositoryConfiguration.MIXIN);
    }

    @Override
    public void validateConfiguration(S3RepositoryConfiguration repositoryConfiguration) {
        if (repositoryConfiguration.getAwsSecretKey().contains("/")) {
            throw new RepositoryConfigurationException("AWS secret keys that contain a '/' are not supported, please use a different key");
        }
        if (repositoryConfiguration.getRoot() != null && !repositoryConfiguration.getRoot().startsWith("/")) {
            throw new RepositoryConfigurationException("root path does not start with /");
        }
    }

    @Override
    public URI getRepositoryUri(Repository repository) {
        S3RepositoryConfiguration configuration = getConfiguration(repository);
        String host = configuration.getBucket().toLowerCase() + ".s3-" + configuration.getRegion().toLowerCase() + ".amazonaws.com";
        return Throwables.returnableInstance(() -> new URI("s3", configuration.getAwsAccessKey() + ":" + configuration.getAwsSecretKey(), host, -1, configuration.getRoot(), null, null));
    }

    @Override
    public Optional<URI> getNewFileUri() {
        return Throwables.returnableInstance(() -> Optional.of(new URI(idGenerator.getId() + ".bin")));

    }

    @Override
    public void delete(Repository repository, RepositoryFile repositoryFile) {
        try {
            S3RepositoryConfiguration configuration = getConfiguration(repository);
            s3Client.deleteObject(new BasicAWSCredentials(configuration.getAwsAccessKey(), configuration.getAwsSecretKey()), configuration.getRegion(), configuration.getBucket(),
                                  getObjectKeyFromRepositoryFile(repositoryFile));
        } catch (OmakaseRuntimeException e) {
            String message = "Failed to delete repository file " + repositoryFile.getId() + " from repository " + repository.getId();
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
        return new MultipartUploadInfo(partSize, ImmutableList.of(Hashes.SHA256, Hashes.MD5_BASE64));
    }

    private static S3RepositoryConfiguration getConfiguration(Repository repository) {
        return (S3RepositoryConfiguration) repository.getRepositoryConfiguration();
    }

    private static String getObjectKeyFromRepositoryFile(RepositoryFile repositoryFile) {
        return repositoryFile.getRelativePath().substring(repositoryFile.getRelativePath().lastIndexOf("/") + 1);
    }
}
