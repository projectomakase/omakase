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
package org.projectomakase.omakase.job;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.jcr.AbstractJcrDAO;
import org.jcrom.Jcrom;
import org.jcrom.util.NodeFilter;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Job DAO.
 *
 * @author Richard Lucas
 */
public class JobDAO extends AbstractJcrDAO<Job> {

    @Inject
    public JobDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    /**
     * Finds any jobs associated to the specified asset id.
     *
     * @param assetId
     *         the asset id
     * @return a list of jobs associated to the specified asset id.
     */
    public List<Job> findJobsForAsset(@NotNull String assetId) {
        String sql = "SELECT job.* " +
                "FROM [omakase:job] AS job " +
                "JOIN [omakase:jobConfiguration] AS config ON ISDESCENDANTNODE(config, job) " +
                "WHERE config.[omakase:variant] IN (" +
                "SELECT variant.[jcr:name] " +
                "FROM [omakase:variant] AS variant " +
                "JOIN [omakase:asset] AS asset ON ISCHILDNODE(variant, asset) " +
                "WHERE asset.[jcr:name] = '" + assetId + "')";
        return super.findBySql(sql, new NodeFilter(NodeFilter.INCLUDE_ALL, NodeFilter.DEPTH_INFINITE));
    }

    /**
     * Finds any jobs associated to the specified variant id.
     *
     * @param variantId
     *         the variant id
     * @return a list of jobs associated to the specified variant id.
     */
    public List<Job> findJobsForVariant(@NotNull String variantId) {
        String sql = "SELECT parent.* " +
                "FROM [omakase:job] AS parent " +
                "JOIN [omakase:jobConfiguration] AS child ON ISDESCENDANTNODE(child, parent) " +
                "WHERE child.[omakase:variant] = '" + variantId + "'";
        return super.findBySql(sql, new NodeFilter(NodeFilter.INCLUDE_ALL, NodeFilter.DEPTH_INFINITE));
    }

    /**
     * Finds any jobs associated to the specified variant id and repository.
     *
     * @param variantId
     *         the variant id
     * @param repositoryId
     *         the repository id
     * @return a list of jobs associated to the specified variant id and repository.
     */
    public List<Job> findJobsForVariantAndRepository(@NotNull String variantId, @NotNull String repositoryId) {
        String sql = "SELECT parent.* " +
                "FROM [omakase:job] AS parent " +
                "JOIN [omakase:jobConfiguration] AS child ON ISDESCENDANTNODE(child, parent) " +
                "WHERE child.[omakase:variant] = '" + variantId + "'" +
                "AND child.[omakase:repositories] = '" + repositoryId + "'" +
                "AND parent.[omakase:type] != 'DELETE'";
        return super.findBySql(sql, new NodeFilter(NodeFilter.INCLUDE_ALL, NodeFilter.DEPTH_INFINITE));
    }
}
