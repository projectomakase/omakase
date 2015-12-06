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
package org.projectomakase.omakase.job.task.jcr;

import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.jcr.AbstractJcrDAO;
import org.jcrom.Jcrom;

import javax.inject.Inject;
import javax.jcr.Session;
import java.util.List;
import java.util.Optional;

/**
 * Task Node JCROM DAO Implementation.
 *
 * @author Richard Lucas
 */
public class TaskNodeDAO extends AbstractJcrDAO<TaskNode> {

    @Inject
    public TaskNodeDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    /**
     * Returns the task for the given id.
     *
     * @param id
     *         the task id
     * @return the task for the given id.
     */
    public Optional<TaskNode> findById(String id) {
        String sql = "SELECT task.* " +
                "FROM [omakase:task] AS task " +
                "WHERE task.[jcr:name]='" + id + "'";
        return findBySql(sql, null).stream().findFirst();
    }

    /**
     * Returns all tasks for the given group.
     *
     * @param taskGroupId
     *         the task group id
     * @return all tasks for the given group.
     */
    public List<TaskNode> findTasksForGroup(String taskGroupId) {
        String sql = "SELECT task.* " +
                "FROM [omakase:task] AS task " +
                "JOIN [omakase:taskGroup] AS group ON ISCHILDNODE(task, group) " +
                "WHERE group.[jcr:name] = '" + taskGroupId + "'" +
                "ORDER BY task.[jcr:created] ASC";
        return findBySql(sql, null);
    }

    /**
     * Returns all tasks that have a status of EXECUTING and belong to the specified worker.
     *
     * @param workerId
     *         the worker id
     * @return all tasks that have a status of EXECUTING and belong to the specified worker.
     */
    public List<TaskNode> findAllExecutingTasksAssociatedToWorker(String workerId) {
        String sql = "SELECT task.* " +
                "FROM [omakase:task] AS task " +
                "JOIN [omakase:worker] as worker " +
                "ON task.[jcr:name] = worker.[omakase:tasks] " +
                "WHERE task.[omakase:status] = 'EXECUTING' " +
                "AND worker.[jcr:name] = '" + workerId + "'";

        return findBySql(sql, null);
    }
}
