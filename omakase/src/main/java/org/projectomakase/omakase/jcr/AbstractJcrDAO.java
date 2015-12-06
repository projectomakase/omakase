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
package org.projectomakase.omakase.jcr;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.exceptions.InvalidUUIDException;
import org.projectomakase.omakase.jcr.query.DefaultSQL2QueryBuilder;
import org.projectomakase.omakase.jcr.query.SQL2QueryBuilder;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchException;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.logging.Logger;
import org.jcrom.Jcrom;
import org.jcrom.callback.JcromCallback;
import org.jcrom.util.NodeFilter;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends Jcrom {@link org.jcrom.dao.AbstractJcrDAO} providing additional common functionality.
 *
 * @author Richard Lucas
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@JcrMappingException
public abstract class AbstractJcrDAO<T> extends org.jcrom.dao.AbstractJcrDAO<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractJcrDAO.class);

    @Inject
    public AbstractJcrDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    @Override
    public T create(String parentNodePath, T entity) {
        T createdEntity = super.create(parentNodePath, entity);
        // this is required to workaround an issue in JCROM where the auto assigned node properties e.g. created/modified are not set on the entity returned by the create method
        return get(jcrom.getPath(createdEntity));
    }

    @Override
    public T create(String parentNodePath, T entity, JcromCallback action) {
        T createdEntity = super.create(parentNodePath, entity, action);
        // this is required to workaround an issue in JCROM where the auto assigned node properties e.g. created/modified are not set on the entity returned by the create method
        return get(jcrom.getPath(createdEntity));
    }

    /**
     * Creates the entity under the parent node path using a distribution algorithm that creates additional hierarchical layers between the parent node path and the entity in order to avoid having a
     * large number of nodes sit under a single parent (too wide).
     * <p>
     * The distribution algorithm uses the provided uuid to create two layers using the first two characters of the uuid for the first layer and the second two characters of the uuid for the second
     * layer. It is assumed the UUID is the jcr node name for the specified entity. For example eb751690-23cb-11e4-8c21-0800200c9a66 would result in the following entity node path
     * /parent/eb/75/eb751690-23cb-11e4-8c21-0800200c9a66. This algorithm allows over 16 million nodes to be stored under a parent node path in a structure that never has more than 256 children under
     * a single node.
     * </p>
     *
     * @param parentNodePath
     *         the path to the parent node
     * @param entity
     *         the object to be mapped to the JCR node
     * @param uuid
     *         the id of the entity, this is expected to be a UUID.
     * @return the newly created object
     */
    public T distributedCreate(String parentNodePath, T entity, String uuid) {

        String nodeLevelOneName = uuid.substring(0, 2);
        String nodeLevelOnePath = parentNodePath + "/" + nodeLevelOneName;

        String nodeLevelTwoName = uuid.substring(2, 4);
        String nodeLevelTwoPath = nodeLevelOnePath + "/" + nodeLevelTwoName;

        String nodeLevelThreeName = uuid.substring(4, 6);
        String nodeLevelThreePath = nodeLevelTwoPath + "/" + nodeLevelThreeName;

        JcrThrowables.wrapJcrExceptions(() -> {
            addLevel(parentNodePath, nodeLevelOneName, nodeLevelOnePath);
            addLevel(nodeLevelOnePath, nodeLevelTwoName, nodeLevelTwoPath);
            addLevel(nodeLevelTwoPath, nodeLevelThreeName, nodeLevelThreePath);
            session.save();
        });

        return create(nodeLevelThreePath, entity);
    }

    /**
     * Returns the node path of a node that was stored under the parent node path using distribution.
     *
     * @param parentNodePath
     *         the path to the parent node
     * @param uuid
     *         the id of the entity, this is expected to be a UUID.
     * @return the node path of a node that was stored under the parent node path using distribution.
     * @throws InvalidUUIDException
     *         if the uuid is not valid.
     */
    public String getDistributedNodePath(String parentNodePath, String uuid) {
        if (uuid.length() < 36) {
            LOGGER.error("Invalid uuid " + uuid);
            throw new InvalidUUIDException("Invalid uuid " + uuid);
        }
        return parentNodePath + "/" + uuid.substring(0, 2) + "/" + uuid.substring(2, 4) + "/" + uuid.substring(4, 6) + "/" + uuid;
    }

    /**
     * Returns all descendant nodes of the root path filtered by node type. The number of rows returned is limited by the count and offset.
     * <p>
     * Uses the {@link DefaultSQL2QueryBuilder} to construct the query.
     * </p>
     *
     * @param rootNodePath
     *         that parent node path
     * @param search
     *         the search that will be used to filter the result set.
     * @param nodeType
     *         the node type
     * @return a {@link SearchResult} containing any matching nodes.
     */
    public SearchResult<T> findNodes(String rootNodePath, Search search, String nodeType) {
        return findNodes(rootNodePath, search, new DefaultSQL2QueryBuilder(nodeType, rootNodePath));
    }

    public SearchResult<T> findNodes(String rootNodePath, Search search, SQL2QueryBuilder queryBuilder) {

        //Validate parentNodePath
        JcrThrowables.wrapJcrExceptions(() -> getNode(rootNodePath));

        try {

            List<T> records = new ArrayList<>();
            queryBuilder.conditions(search.getSearchConditions()).orderBy(search.getOrderBy(), search.getSortOrder());

            QueryManager queryManager = getSession().getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryBuilder.build(), Query.JCR_SQL2);
            QueryResult result = query.execute();

            QueryResult resultWithLimit = null;
            if (!search.isOnlyCount()) {
                if (search.getCount() > 0) {
                    queryBuilder.limit(search.getCount(), search.getOffset());
                }
                Query queryWithLimit = queryManager.createQuery(queryBuilder.build(), Query.JCR_SQL2);
                resultWithLimit = queryWithLimit.execute();
            }

            long totalCount = -1;
            if (result != null) {
                totalCount = result.getNodes().getSize();
            }

            // totalCount > search.getOffset() is a workaround for MODE-2435
            if (resultWithLimit != null && totalCount > search.getOffset()) {
                records = toList(resultWithLimit.getNodes(), null);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(" plan -> " + ((org.modeshape.jcr.api.query.QueryResult)result).getPlan());
            }

            return new SearchResult<>(records, totalCount);
        } catch (RepositoryException e) {
            throw new SearchException(e.getMessage(), e);
        }
    }

    @Override
    public T update(T entity) {
        T updatedEntity = super.update(entity);
        // this is required to workaround an issue in JCROM where the auto assigned node properties e.g. created/modified are not updated on the entity returned by the update method
        return get(jcrom.getPath(updatedEntity));
    }

    @Override
    public T update(T entity, NodeFilter nodeFilter) {
        T updatedEntity = super.update(entity, nodeFilter);
        // this is required to workaround an issue in JCROM where the auto assigned node properties e.g. created/modified are not updated on the entity returned by the update method
        return get(jcrom.getPath(updatedEntity));
    }

    private void addLevel(String currentNodeLevelPath, String nextNodeLevelName, String nextNodeLevelPath) throws RepositoryException {
        if (!session.nodeExists(nextNodeLevelPath)) {
            Node parentNode = session.getNode(currentNodeLevelPath);
            parentNode.addNode(nextNodeLevelName);
        }
    }
}
