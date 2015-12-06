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
package org.projectomakase.omakase.job.pipeline;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.broker.BrokerManager;
import org.projectomakase.omakase.broker.Capacity;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.RepositorySearchBuilder;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class JobScenarioTools {

    private static final Logger LOGGER = Logger.getLogger(JobScenarioTools.class);

    @Inject
    JobManager jobManager;
    @Inject
    TaskManager taskManager;
    @Inject
    BrokerManager brokerManager;
    @Inject
    RepositoryManager repositoryManager;

    public void consumeTasks(Worker worker, int numberOfTasks, String taskType, List<TaskStatusUpdate> taskStatusUpdates) {
        IntStream.range(0, numberOfTasks).forEach(i -> {
            Task task = getNextAvailableTask(worker, taskType);
            brokerManager.handleTaskStatusUpdateFromWorker(worker.getId(), task.getId(), taskStatusUpdates.get(i));
        });
    }

    public void consumeTasks(Worker worker, int numberOfTasks, String taskType, Function<Integer, TaskStatusUpdate> taskStatusUpdateFunction) {
        IntStream.range(0, numberOfTasks).forEach(i -> {
            Task task = getNextAvailableTask(worker, taskType);
            brokerManager.handleTaskStatusUpdateFromWorker(worker.getId(), task.getId(), taskStatusUpdateFunction.apply(i));
        });
    }

    public Task getNextAvailableTask(Worker worker, String taskType) {
        ImmutableList<Task> tasks = brokerManager.getNextAvailableTasksForWorker(worker.getId(), ImmutableList.of(new Capacity(taskType, 1)));
        assertThat(tasks).hasSize(1);
        Task task = tasks.stream().findFirst().get();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.EXECUTING);
        return task;
    }

    public void waitUntilJobHasStatus(String jobId, JobStatus jobStatus) {
        // tries for 15 seconds before giving up
        for (int i = 0; i < 60; i++) {
            if (jobManager.getJob(jobId).get().getStatus().equals(jobStatus)) {
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public void waitUntilJobsHasTaskGroupWithStatus(String jobId, TaskStatus taskStatus) {
        // tries for 15 seconds before giving up
        for (int i = 0; i < 5; i++) {
            if (taskManager.getTaskGroups(jobId).stream().anyMatch(taskGroup -> taskStatus.equals(taskGroup.getStatus()))) {
                break;
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public void waitUntilTaskGroupHasStatus(String taskGroupId, TaskStatus taskStatus) {
        // tries for 15 seconds before giving up
        for (int i = 0; i < 30; i++) {
            if (taskManager.getTaskGroup(taskGroupId).get().getStatus().equals(taskStatus)) {
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public TaskGroup getQueuedTaskGroupForJob(Job job) {
        return taskManager.getTaskGroups(job.getId()).stream().filter(taskGroup -> TaskStatus.QUEUED.equals(taskGroup.getStatus())).findFirst().orElseThrow(
                () -> new OmakaseRuntimeException("QUEUED Task Group expected"));
    }

    public Repository getRepositoryByName(String repositoryName) {
        return repositoryManager.findRepositories(
                new RepositorySearchBuilder()
                        .conditions(ImmutableList.of(new SearchCondition(Repository.REPOSITORY_NAME, Operator.EQ, repositoryName)))
                        .build())
                .getRecords()
                .stream().findFirst().orElseThrow(OmakaseRuntimeException::new);
    }
}
