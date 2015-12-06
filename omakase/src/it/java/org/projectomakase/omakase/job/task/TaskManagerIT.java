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
package org.projectomakase.omakase.job.task;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.task.jcr.TaskNodeDAO;
import org.projectomakase.omakase.job.task.queue.TaskQueue;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SearchResult;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class TaskManagerIT {

    @Inject
    TaskManager taskManager;
    @Inject
    TaskNodeDAO taskDAO;
    @Inject
    IdGenerator idGenerator;
    @Inject
    IntegrationTests integrationTests;
    @Inject
    TaskQueue taskQueue;

    private String jobId;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() {
        TestRunner.runAsUser("admin", "password", this::setup);
    }

    @After
    public void after() {
        TestRunner.runAsUser("admin", "password", this::cleanup);
    }

    @Test
    public void shouldCreateTaskGroup() {
        TestRunner.runAsUser("admin", "password", () -> {
            String workflowId = idGenerator.getId();
            TaskGroup taskGroup = taskManager.createTaskGroup(newTaskGroup(jobId, workflowId));
            validateTaskGroup(workflowId, taskGroup);
        });
    }

    @Test
    public void shouldCreateTask() {
        TestRunner.runAsUser("admin", "password", () -> {
            String workflowId = idGenerator.getId();
            TaskGroup taskGroup = taskManager.createTaskGroup(newTaskGroup(jobId, workflowId));
            Task task = taskManager.createTask(taskGroup.getId(), newTask());
            Throwables.voidInstance(()-> validateTask(task));
            // the task group should only contain the newly created task
            Assertions.assertThat(taskManager.getTasks(taskGroup.getId())).extracting("id").containsExactly(task.getId());
        });
    }

    @Test
    public void shouldGetTasksForGroup() {
        TestRunner.runAsUser("admin", "password", () -> {
            String workflowId = idGenerator.getId();
            TaskGroup taskGroup = taskManager.createTaskGroup(newTaskGroup(jobId, workflowId));
            taskManager.createTask(taskGroup, newTask());
            Assertions.assertThat(taskManager.getTasks(taskGroup.getId())).hasSize(1);
        });
    }

    @Test
    public void shouldGetTaskGroup() {
        TestRunner.runAsUser("admin", "password", () -> {
            String workflowId = idGenerator.getId();
            TaskGroup taskGroup = taskManager.createTaskGroup(newTaskGroup(jobId, workflowId));
            TaskGroup retrievedTaskGroup = taskManager.getTaskGroup(taskGroup.getId()).get();
            validateTaskGroup(workflowId, retrievedTaskGroup);
        });
    }

    @Test
    public void shouldFindTasks() {
        TestRunner.runAsUser("admin", "password", () -> {
            String workflowId = idGenerator.getId();
            TaskGroup taskGroup1 = taskManager.createTaskGroup(newTaskGroup(jobId, workflowId));
            Task task1 = taskManager.createTask(taskGroup1, newTask());

            String jobId2 = integrationTests.createJob();
            TaskGroup taskGroup2 = taskManager.createTaskGroup(newTaskGroup(jobId2, workflowId));
            Task task2 = taskManager.createTask(taskGroup2, newTask());
            taskManager.updateTaskStatus(task2.getId(), new TaskStatusUpdate(TaskStatus.EXECUTING, "", -1));

            String jobId3 = integrationTests.createJob();
            TaskGroup taskGroup3 = taskManager.createTaskGroup(newTaskGroup(jobId3, workflowId));
            Task task3 = taskManager.createTask(taskGroup3, newTask());
            taskManager.updateTaskStatus(task3.getId(), new TaskStatusUpdate(TaskStatus.FAILED_CLEAN, "", -1));

            SearchResult<Task> searchResults = taskManager.findJobs(new TaskSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS, Operator.EQ, "QUEUED,EXECUTING"))).build());
            assertThat(searchResults.getTotalRecords()).isEqualTo(2);
            assertThat(searchResults.getRecords()).extracting("id").containsOnly(task1.getId(), task2.getId());

            SearchResult<Task> searchResults2 = taskManager.findJobs(new TaskSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS, Operator.EQ, "FAILED_CLEAN"))).build());
            assertThat(searchResults2.getTotalRecords()).isEqualTo(1);
            assertThat(searchResults2.getRecords()).extracting("id").containsOnly(task3.getId());
        });
    }

    private TaskGroup newTaskGroup(String jobId, String workflowId) {
        return new TaskGroup(jobId, workflowId, "test");
    }

    private Task newTask() {
        return Throwables.returnableInstance(() -> new Task("TRANSFER", "Test Task", 1,
                new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(new URI("file:/test.txt"), new URI("file:/dest.txt"))), Collections.singletonList("MD5"))));
    }

    private void validateTaskGroup(String workflowId, TaskGroup taskGroup) {
        assertThat(taskGroup.getJobId()).isEqualTo(jobId);
        assertThat(taskGroup.getPipelineId()).isEqualTo(workflowId);
        assertThat(taskGroup.getJobId()).isEqualTo(jobId);
        assertThat(taskGroup.getCallbackListenerId()).isEqualTo("test");
    }

    private void validateTask(Task task) throws Exception {
        assertThat(task.getType()).isEqualTo("TRANSFER");
        assertThat(task.getDescription()).isEqualTo("Test Task");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.QUEUED);
        assertThat(task.getPriority()).isEqualTo(1L);
        TransferTaskConfiguration configuration = (TransferTaskConfiguration) task.getConfiguration();
        assertThat(configuration.getIoInstructions()).hasSize(1);
        assertThat(configuration.getIoInstructions().get(0)).isEqualToComparingFieldByField(new IOInstruction(new URI("file:/test.txt"), new URI("file:/dest.txt")));
        assertThat(configuration.getHashAlgorithms()).contains("MD5");
        // Check that a message has been added to the tool queue for the task
        assertThat(taskQueue.get(task.getType()).get()).isEqualTo(task.getId());
    }

    private void setup() {
        cleanup();
        jobId = integrationTests.createJob();
    }

    private void cleanup() {
        integrationTests.cleanup();
        jobId = null;
    }
}