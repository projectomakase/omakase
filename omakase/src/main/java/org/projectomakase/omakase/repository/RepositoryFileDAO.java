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

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.jcr.AbstractJcrDAO;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.jcrom.Jcrom;
import org.jcrom.util.NodeFilter;

import javax.inject.Inject;
import javax.jcr.Session;
import java.util.List;
import java.util.Optional;

/**
 * Repository File DAO.
 *
 * @author Richard Lucas
 */
public class RepositoryFileDAO extends AbstractJcrDAO<RepositoryFile> {

    @Inject
    public RepositoryFileDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    public List<RepositoryFile> findByVariantIdAndRepositoryId(final String variantId, final String repositoryId) {
        String sql = "SELECT file.* " +
                "FROM [omakase:repositoryFile] AS file " +
                "JOIN [omakase:repository] AS repository ON ISDESCENDANTNODE(file, repository) " +
                "WHERE file.[omakase:variantId] = '" + variantId + "' " +
                "AND repository.[jcr:name] = '" + repositoryId + "'";
        return  super.findBySql(sql, new NodeFilter(NodeFilter.INCLUDE_ALL, NodeFilter.DEPTH_INFINITE));
    }

    public Optional<RepositoryFile> findByVariantFileIdAndRepositoryId(final String variantFileId, final String repositoryId) {
        String sql = "SELECT file.* " +
                "FROM [omakase:repositoryFile] AS file " +
                "JOIN [omakase:repository] AS repository ON ISDESCENDANTNODE(file, repository) " +
                "WHERE file.[omakase:variantFileId] = '" + variantFileId + "' " +
                "AND repository.[jcr:name] = '" + repositoryId + "'";
        return  super.findBySql(sql, new NodeFilter(NodeFilter.INCLUDE_ALL, NodeFilter.DEPTH_INFINITE)).stream().findFirst();
    }
}
