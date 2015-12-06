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
package org.projectomakase.omakase.broker;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.collectors.ImmutableListsCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageSearchBuilder;
import org.projectomakase.omakase.job.message.MessageType;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;
import org.projectomakase.omakase.task.providers.delete.DeleteTaskConfiguration;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.assertj.core.groups.Tuple;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class BrokerManagerIT {

    private static final Logger LOGGER = Logger.getLogger(BrokerManagerIT.class);

    @Inject
    BrokerManager brokerManager;
    @Inject
    TaskManager taskManager;
    @Inject
    IntegrationTests integrationTests;
    @Inject
    IdGenerator idGenerator;
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
    public void shouldPreRegisterWorker() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> w.setStatus(WorkerStatus.STARTING)));
            assertThat(registeredWorker.getId()).isNotNull();
            assertThat(registeredWorker.getWorkerName()).isNull();
            assertThat(registeredWorker.getExternalIds()).isEmpty();
            assertThat(registeredWorker.getStatus()).isEqualTo(WorkerStatus.STARTING);
            assertThat(registeredWorker.getStatusTimestamp()).isToday();
            assertThatWorkerHasCreatedAndLastModified(registeredWorker, "admin");
        });
    }

    @Test
    public void shouldRegisterWorker() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> {
                w.setWorkerName("worker 1");
                w.setExternalIds(ImmutableList.of("a", "b"));
            }));
            assertThat(registeredWorker.getId()).isNotNull();
            assertThat(registeredWorker.getWorkerName()).isEqualTo("worker 1");
            assertThat(registeredWorker.getExternalIds()).containsExactly("a", "b");
            assertThat(registeredWorker.getStatus()).isEqualTo(WorkerStatus.ACTIVE);
            assertThat(registeredWorker.getStatusTimestamp()).isToday();
            assertThatWorkerHasCreatedAndLastModified(registeredWorker, "admin");
        });
    }

    @Test
    public void shouldFindWorkers() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                int index = i;
                brokerManager.registerWorker(Worker.Builder.build(w -> {
                    w.setWorkerName("worker" + index);
                    w.setExternalIds(ImmutableList.of("a", "b"));
                }));
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().build()).getRecords()).extracting("workerName").containsExactly("worker4", "worker3", "worker2", "worker1", "worker0");
        });
    }

    @Test
    public void shouldGetWorker() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> {
                w.setWorkerName("worker 1");
                w.setExternalIds(ImmutableList.of("a", "b"));
            }));
            assertThat(brokerManager.getWorker(registeredWorker.getId()).get()).isEqualToComparingFieldByField(registeredWorker);
        });
    }

    @Test
    public void shouldUpdateWorker() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> w.setStatus(WorkerStatus.STARTING)));
            registeredWorker.setWorkerName("test");
            registeredWorker.setStatus(WorkerStatus.ACTIVE);
            Worker updatedWorker = brokerManager.updateWorker(registeredWorker);
            assertThat(updatedWorker.getWorkerName()).isEqualTo("test");
            assertThat(updatedWorker.getStatus()).isEqualTo(WorkerStatus.ACTIVE);
        });
    }

    @Test
    public void shouldUnregisterWorker() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {

            Task executingOne = updateTaskStatus(createTransferTask(), TaskStatus.EXECUTING);
            Task executingTwo = updateTaskStatus(createTransferTask(), TaskStatus.EXECUTING);
            Task failed = updateTaskStatus(createTransferTask(), TaskStatus.FAILED_DIRTY);
            Task completed = updateTaskStatus(createTransferTask(), TaskStatus.COMPLETED);

            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> {
                w.setWorkerName("worker 1");
                w.setExternalIds(ImmutableList.of("a", "b"));
                w.setTasks(ImmutableList.of(executingOne.getId(), executingTwo.getId(), failed.getId(), completed.getId()));
            }));

            assertThat(brokerManager.getWorker(registeredWorker.getId()).isPresent()).isTrue();
            brokerManager.unregisterWorker(registeredWorker.getId());
            assertThat(brokerManager.getWorker(registeredWorker.getId()).isPresent()).isFalse();

            // validate the executing tasks where failed
            assertThat(taskManager.getTask(executingOne.getId()).get().getStatus()).isEqualTo(TaskStatus.FAILED_DIRTY);
            assertThat(taskManager.getTask(executingTwo.getId()).get().getStatus()).isEqualTo(TaskStatus.FAILED_DIRTY);
            assertThat(taskManager.getTask(failed.getId()).get().getStatus()).isEqualTo(TaskStatus.FAILED_DIRTY);
            assertThat(taskManager.getTask(completed.getId()).get().getStatus()).isEqualTo(TaskStatus.COMPLETED);
        });
    }

    @Test
    public void shouldGetNextAvailableTasksForWorker() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> w.setWorkerName("worker 1")));
            assertThat(brokerManager.getWorker(registeredWorker.getId()).isPresent()).isTrue();
            ImmutableList.Builder<Task> tasksBuilder = ImmutableList.builder();
            for (int i = 0; i < 5; i++) {
                tasksBuilder.add(createTransferTask());
            }
            ImmutableList<Task> tasks = tasksBuilder.build();
            ImmutableList<Task> availableTasks = brokerManager.getNextAvailableTasksForWorker(registeredWorker.getId(), ImmutableList.of(new Capacity("TRANSFER", 1)));
            assertThat(availableTasks).hasSize(1);
            assertThat(availableTasks.get(0).getId()).isEqualTo(tasks.get(0).getId());
            assertThat(availableTasks.get(0).getStatus()).isEqualTo(TaskStatus.EXECUTING);

            availableTasks = brokerManager.getNextAvailableTasksForWorker(registeredWorker.getId(), ImmutableList.of(new Capacity("TRANSFER", 4)));
            assertThat(availableTasks).hasSize(4);
            assertThat(availableTasks).extracting("id", "status")
                    .contains(new Tuple(tasks.get(1).getId(), TaskStatus.EXECUTING), new Tuple(tasks.get(2).getId(), TaskStatus.EXECUTING), new Tuple(tasks.get(3).getId(), TaskStatus.EXECUTING),
                              new Tuple(tasks.get(4).getId(), TaskStatus.EXECUTING));

            assertThat(brokerManager.getNextAvailableTasksForWorker(registeredWorker.getId(), ImmutableList.of(new Capacity("TRANSFER", 4)))).isEmpty();
        });
    }

    @Test
    public void shouldOnlyGetNextAvailableTasksForWorkerThatMatchesRequestedType() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> w.setWorkerName("worker 1")));
            assertThat(brokerManager.getWorker(registeredWorker.getId()).isPresent()).isTrue();
            createDeleteTask();
            ImmutableList<Task> availableTasks = brokerManager.getNextAvailableTasksForWorker(registeredWorker.getId(), ImmutableList.of(new Capacity("TRANSFER", 1)));
            assertThat(availableTasks).hasSize(0);
        });
    }

    @Test
    public void shouldNotAssignTaskToStoppingWorker() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> {
                w.setWorkerName("worker 1");
                w.setStatus(WorkerStatus.STOPPING);
            }));
            createTransferTask();
            assertThat(brokerManager.getNextAvailableTasksForWorker(registeredWorker.getId(), ImmutableList.of(new Capacity("TRANSFER", 1)))).isEmpty();
        });
    }

    @Test
    public void shouldFindMessagesForWorker() {
        TestRunner.runAsUser("admin", "password", () -> {

            List<String> tasks = IntStream.range(0, 5).mapToObj(i -> {
                Task task = createTransferTask();
                taskManager.addMessageToTask(task.getId(), new Message("info message " + i, MessageType.INFO));
                taskManager.addMessageToTask(task.getId(), new Message("error message " + i, MessageType.ERROR));
                return task.getId();
            }).collect(ImmutableListCollector.toImmutableList());

            Worker registeredWorker = brokerManager.registerWorker(Worker.Builder.build(w -> {
                w.setWorkerName("worker 1");
                w.setTasks(tasks);
            }));

            List<Message> expectedMessages =
                    IntStream.of(4, 3, 2, 1, 0).mapToObj(i -> ImmutableList.of(new Message("error message " + i, MessageType.ERROR), new Message("info message " + i, MessageType.INFO)))
                            .collect(ImmutableListsCollector.toImmutableList());
            List<Message> actualMessages = brokerManager.findWorkerMessages(registeredWorker.getId(), new MessageSearchBuilder().count(-1).build()).getRecords();
            assertThat(actualMessages).hasSize(10).usingElementComparatorOnFields("messageValue", "messageType").containsExactlyElementsOf(expectedMessages);
        });
    }

    private Task createTransferTask() {
        TaskGroup createdTaskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, idGenerator.getId(), "test"));
        return Throwables.returnableInstance(() -> taskManager.createTask(createdTaskGroup.getId(), new Task("TRANSFER", "test", 1,
                                                                                                             new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(new URI("file:/test.txt"),
                                                                                                                                                                              new URI("file:/dest.txt"))),
                                                                                                                                           Collections.singletonList("MD5")))));
    }

    private Task createDeleteTask() {
        TaskGroup createdTaskGroup = taskManager.createTaskGroup(new TaskGroup(jobId, idGenerator.getId(), "test"));
        return Throwables
                .returnableInstance(() -> taskManager.createTask(createdTaskGroup.getId(), new Task("DELETE", "test", 1, new DeleteTaskConfiguration(ImmutableList.of(new URI("file:/test.txt"))))));
    }


    private Task updateTaskStatus(Task task, TaskStatus taskStatus) {
        return taskManager.updateTaskStatus(task.getId(), new TaskStatusUpdate(taskStatus, "", -1));
    }

    private void assertThatWorkerHasCreatedAndLastModified(Worker worker, String expectedUser) {
        assertThat(worker.getCreated()).isToday();
        assertThat(worker.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(worker.getLastModified()).isToday();
        assertThat(worker.getLastModifiedBy()).isEqualTo(expectedUser);
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