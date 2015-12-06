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
package org.projectomakase.omakase.repository.provider.ftp;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.jcr.JcrTools;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.spi.RepositoryProvider;

import javax.inject.Inject;
import javax.jcr.nodetype.PropertyDefinition;
import java.net.URI;
import java.util.Optional;

/**
 * FTP Repository Provider implementation.
 *
 * @author Scott Sharp
 */
public class FtpRepositoryProvider implements RepositoryProvider<FtpRepositoryConfiguration> {

    private static final String TYPE = "FTP";

    @Inject
    JcrTools jcrTools;
    @Inject
    IdGenerator idGenerator;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Class<FtpRepositoryConfiguration> getConfigurationType() {
        return FtpRepositoryConfiguration.class;
    }

    @Override
    public ImmutableSet<PropertyDefinition> getConfigurationTemplate() {
        return jcrTools.getPropertyDefinitions(FtpRepositoryConfiguration.MIXIN);
    }

    @Override
    public void validateConfiguration(FtpRepositoryConfiguration repositoryConfiguration) {
        getRepositoryUri(repositoryConfiguration);
    }

    @Override
    public URI getRepositoryUri(Repository repository) {
        return getRepositoryUri((FtpRepositoryConfiguration) repository.getRepositoryConfiguration());
    }

    @Override
    public Optional<URI> getNewFileUri() {
        String id = idGenerator.getId();
        String path = String.join("/", id.substring(0, 2), id.substring(2, 4), id.substring(4, 6), id + ".bin");
        return Throwables.returnableInstance(() -> Optional.of(new URI(path)));
    }

    public URI getRepositoryUri(FtpRepositoryConfiguration repositoryConfiguration) {
        if (repositoryConfiguration.getPassive()) {
            return Throwables.returnableInstance(() -> new URI("ftp", repositoryConfiguration.getUsername() + ":" + repositoryConfiguration.getPassword(), repositoryConfiguration.getAddress(),
                                                               Ints.checkedCast(repositoryConfiguration.getPort()), repositoryConfiguration.getRoot(), "passive=true", null));
        } else {
            return Throwables.returnableInstance(() -> new URI("ftp", repositoryConfiguration.getUsername() + ":" + repositoryConfiguration.getPassword(), repositoryConfiguration.getAddress(),
                                                               Ints.checkedCast(repositoryConfiguration.getPort()), repositoryConfiguration.getRoot(), null, null));
        }
    }
}
