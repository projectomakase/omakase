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
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SortOrder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class WorkerSearchIT {

    @Inject
    BrokerManager brokerManager;
    @Inject
    IntegrationTests integrationTests;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @After
    public void after() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @Test
    public void shouldFindWorkersOrderedByDefault() {
        // default is created DESC
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().build()).getRecords()).extracting("workerName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindWorkersOrderedByNameAscending() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().orderBy(Worker.WORKER_NAME).sortOrder(SortOrder.ASC).build()).getRecords()).extracting("workerName")
                    .containsExactly("test0", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void shouldFindWorkersOrderedByNameDescending() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().orderBy(Worker.WORKER_NAME).sortOrder(SortOrder.DESC).build()).getRecords()).extracting("workerName")
                    .containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindWorkersWhereIdEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.ID, Operator.EQ, worker.getId()))).build()).getRecords())
                    .extracting("id").containsExactly(worker.getId());
        });
    }

    @Test
    public void shouldFindWorkersWhereNameEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.WORKER_NAME, Operator.EQ, "test3"))).build()).getRecords())
                    .extracting("workerName").containsExactly("test3");
        });
    }

    @Test
    public void shouldFindWorkersWhereNameLike() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.WORKER_NAME, Operator.LIKE, "3"))).build()).getRecords())
                    .extracting("workerName").containsExactly("test3");
        });
    }

    @Test
    public void shouldFindWorkersWhereExternalIdEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                worker.setExternalIds(ImmutableList.of(Integer.toString(i), "abc"));
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.EXTERNAL_IDS, Operator.EQ, "3"))).build()).getRecords())
                    .extracting("workerName").containsExactly("test3");
        });
    }

    @Test
    public void shouldFindWorkersStatusEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Worker worker = getWorker();
                worker.setWorkerName("test" + i);
                brokerManager.registerWorker(worker);
            }
            assertThat(brokerManager.findWorkers(
                    new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS, Operator.EQ, "ACTIVE"))).orderBy(Worker.WORKER_NAME).sortOrder(SortOrder.DESC).build())
                    .getRecords()).extracting("workerName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampEqualsDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.EQ, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampEqualsDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.EQ, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampGreaterThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.GT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampGreaterThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.GT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampGreaterThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.GTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampGreaterThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.GTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampLessThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.LT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampLessThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.LT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampLessThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.LTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersStatusTimeStampLessThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.STATUS_TIMESTAMP, Operator.LTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedEqualsDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.EQ, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedEqualsDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.EQ, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedGreaterThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.GT, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedGreaterThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.GT, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedGreaterThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.GTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedGreaterThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.GTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedLessThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.LT, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedLessThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.LT, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedLessThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.LTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedLessThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED, Operator.LTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedByEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED_BY, Operator.EQ, "admin"))).build()).getRecords().get(0))
                    .isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersCreatedByNotEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.CREATED_BY, Operator.NE, "admin1"))).build()).getRecords().get(0))
                    .isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedEqualsDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.EQ, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedEqualsDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.EQ, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedGreaterThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.GT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedGreaterThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.GT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedGreaterThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.GTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedGreaterThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.GTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedLessThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.LT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedLessThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.LT, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedLessThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.LTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedLessThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED, Operator.LTE, dateSearchValue, true))).build())
                    .getRecords().get(0)).isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedByEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            assertThat(
                    brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED_BY, Operator.EQ, "admin"))).build()).getRecords().get(0))
                    .isEqualToComparingFieldByField(worker);
        });
    }

    @Test
    public void shouldFindWorkersLastModifiedByNotEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Worker worker = brokerManager.registerWorker(getWorker());
            assertThat(
                    brokerManager.findWorkers(new WorkerSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Worker.LAST_MODIFIED_BY, Operator.NE, "admin1"))).build()).getRecords().get(0))
                    .isEqualToComparingFieldByField(worker);
        });
    }


    private Worker getWorker() {
        return Worker.Builder.build(w -> {
            w.setWorkerName("test");
            w.setExternalIds(ImmutableList.of("a", "b"));
        });
    }

}