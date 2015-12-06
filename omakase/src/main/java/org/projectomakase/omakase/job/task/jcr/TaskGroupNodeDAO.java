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
 * Task Group DAO.
 *
 * @author Richard Lucas
 */
public class TaskGroupNodeDAO extends AbstractJcrDAO<TaskGroupNode> {

    @Inject
    public TaskGroupNodeDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    /**
     * Returns all of the task groups for a given job id.
     *
     * @param jobId
     *         the job id
     * @return all of the task groups for a given job id.
     */
    public List<TaskGroupNode> findTaskGroupsForJob(String jobId) {
        String sql = "SELECT group.* " +
                "FROM [omakase:taskGroup] AS group " +
                "JOIN [omakase:job] AS job ON ISCHILDNODE(group, job) " +
                "WHERE job.[jcr:name]='" + jobId + "'";
        return findBySql(sql, null);
    }

    /**
     * Returns the task group for the given id if it exists, otherwise returns an empty Optional.
     *
     * @param id
     *         the task group id
     * @return the task group for the given id.
     */
    public Optional<TaskGroupNode> findById(String id) {
        String sql = "SELECT group.* " +
                "FROM [omakase:taskGroup] AS group " +
                "WHERE group.[jcr:name]='" + id + "'";
        return findBySql(sql, null).stream().findFirst();
    }

    /**
     * Returns the task group for the given task id if it exists, otherwise returns an empty Optional.
     *
     * @param taskId
     *         the task id of a task that belongs to the task group
     * @return the task group for the given task id.
     */
    public Optional<TaskGroupNode> findByTaskId(String taskId) {
        String sql = "SELECT group.* " +
                "FROM [omakase:taskGroup] AS group " +
                "JOIN [omakase:task] AS task ON ISCHILDNODE(task, group) " +
                "WHERE task.[jcr:name]='" + taskId + "'";
        return findBySql(sql, null).stream().findFirst();
    }
}
