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
package org.projectomakase.omakase.job.message;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.jcr.AbstractJcrDAO;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.jcrom.Jcrom;

import javax.inject.Inject;
import javax.jcr.Session;

/**
 * @author Richard Lucas
 */
public class MessageDAO extends AbstractJcrDAO<Message> {

    @Inject
    @OrganizationNodePath("jobs")
    String jobsNodePath;

    @Inject
    public MessageDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    /**
     * Returns all of the messages, belonging to the specified job that match the given search criteria.
     *
     * @param jobNodePath the job's node path
     * @param search the search criteria
     * @return a {@link SearchResult} containing any matching messages.
     */
    public SearchResult<Message> findJobMessages(String jobNodePath, Search search) {
        return findNodes(jobNodePath, search, "omakase:message");
    }

    /**
     * Returns all of the messages, belonging to the specified worker that match the given search criteria.
     *
     * @param workerId the job's node path
     * @param search the search criteria
     * @return a {@link SearchResult} containing any matching messages.
     */
    public SearchResult<Message> findWorkerMessages(String workerId, Search search) {
        return findNodes(jobsNodePath, search, new WorkerMessageSQL2QueryBuilder(workerId));
    }

}
