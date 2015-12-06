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

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
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

/**
 * @author Richard Lucas
 */
@RunWith(Arquillian.class)
public class JobSearchIT {

    @Inject
    JobManager jobManager;
    @Inject
    ContentManager contentManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    IntegrationTests integrationTests;

    private Variant variant;
    private Repository configuredRepository;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() throws Exception {
        TestRunner.runAsUser("admin", "password", this::setup);
        integrationTests.destroyJCRSession();
    }

    @After
    public void after() {
        integrationTests.destroyJCRSession();
        TestRunner.runAsUser("admin", "password", this::cleanup);
    }

    @Test
    public void shouldFindJobsOrderedByDefault() {
        // default is created DESC
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(jobManager.findJobs(new JobSearchBuilder().build()).getRecords()).extracting("jobName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindJobsOrderedByNameAscending() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(jobManager.findJobs(new JobSearchBuilder().orderBy(Job.JOB_NAME).sortOrder(SortOrder.ASC).build()).getRecords()).extracting("jobName")
                    .containsExactly("test0", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void shouldFindJobsOrderedByNameDescending() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(jobManager.findJobs(new JobSearchBuilder().orderBy(Job.JOB_NAME).sortOrder(SortOrder.DESC).build()).getRecords()).extracting("jobName")
                    .containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindJobsWhereIdEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.ID, Operator.EQ, job.getId()))).build()).getRecords()).extracting("id")
                    .containsExactly(job.getId());
        });
    }

    @Test
    public void shouldFindJobsWhereNameEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.JOB_NAME, Operator.EQ, "test3"))).build()).getRecords()).extracting("jobName")
                    .containsExactly("test3");
        });
    }

    @Test
    public void shouldFindJobsWhereNameLike() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.JOB_NAME, Operator.LIKE, "3"))).build()).getRecords()).extracting("jobName")
                    .containsExactly("test3");
        });
    }

    @Test
    public void shouldFindJobsWhereExternalIdEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                job.setExternalIds(ImmutableList.of(Integer.toString(i), "abc"));
                jobManager.createJob(job);
            }
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.EXTERNAL_IDS, Operator.EQ, "3"))).build()).getRecords()).extracting("jobName")
                    .containsExactly("test3");
        });
    }

    @Test
    public void shouldFindJobsTypeEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.TYPE, Operator.EQ, "INGEST"))).orderBy(Job.JOB_NAME).sortOrder(SortOrder.DESC).build())
                            .getRecords()).extracting("jobName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindJobsStatusEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                Job job = getJob();
                job.setJobName("test" + i);
                jobManager.createJob(job);
            }
            assertThat(jobManager
                    .findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS, Operator.EQ, "UNSUBMITTED"))).orderBy(Job.JOB_NAME).sortOrder(SortOrder.DESC).build())
                    .getRecords()).extracting("jobName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampEqualsDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.EQ, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampEqualsDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);

            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.EQ, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampGreaterThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.GT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampGreaterThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.GT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampGreaterThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.GTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampGreaterThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.GTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampLessThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.LT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampLessThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.LT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampLessThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.LTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsStatusTimeStampLessThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.STATUS_TIMESTAMP, Operator.LTE, dateSearchValue, true))).build()).getRecords()
                    .get(0)).isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsPriorityEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.PRIORITY, Operator.EQ, "4"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsPriorityGreaterThan() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            job.setPriority(7);
            job = jobManager.createJob(job);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.PRIORITY, Operator.GT, "4"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsPriorityGreaterThanOrEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            job.setPriority(7);
            job = jobManager.createJob(job);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.PRIORITY, Operator.GTE, "7"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsPriorityLessThan() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            job.setPriority(2);
            job = jobManager.createJob(job);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.PRIORITY, Operator.LT, "4"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsPriorityLessThanOrEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            job.setPriority(2);
            job = jobManager.createJob(job);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.PRIORITY, Operator.LTE, "2"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsSystemEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = getJob();
            job.setSystem(true);
            job = jobManager.createJob(job);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.SYSTEM, Operator.EQ, "true"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsSystemNotEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.SYSTEM, Operator.NE, "true"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }


    @Test
    public void shouldFindJobsCreatedEqualsDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.EQ, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedEqualsDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.EQ, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedGreaterThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.GT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedGreaterThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.GT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedGreaterThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.GTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedGreaterThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.GTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedLessThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.LT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedLessThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.LT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedLessThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.LTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedLessThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED, Operator.LTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedByEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED_BY, Operator.EQ, "admin"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsCreatedByNotEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.CREATED_BY, Operator.NE, "admin1"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedEqualsDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.EQ, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedEqualsDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.EQ, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedGreaterThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.GT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedGreaterThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.GT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedGreaterThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.GTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedGreaterThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.minusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.GTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedLessThanDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.LT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedLessThanDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.LT, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedLessThanOrEqualDate() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusDays(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE);

            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.LTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedLessThanOrEqualDateTime() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            LocalDateTime localDateTime = LocalDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault());
            localDateTime = localDateTime.plusMinutes(1);
            String dateSearchValue = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
            assertThat(
                    jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED, Operator.LTE, dateSearchValue, true))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedByEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED_BY, Operator.EQ, "admin"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    @Test
    public void shouldFindJobsLastModifiedByNotEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            Job job = jobManager.createJob(getJob());
            assertThat(jobManager.findJobs(new JobSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Job.LAST_MODIFIED_BY, Operator.NE, "admin1"))).build()).getRecords().get(0))
                    .isEqualToIgnoringGivenFields(job, "jobConfiguration");
        });
    }

    private void setup() {
        cleanup();
        Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of()));
        variant = contentManager.createVariant(asset.getId(), new Variant("test", ImmutableList.of()));
        configuredRepository = repositoryManager.createRepository(new Repository("configured", "configured", "FILE"));
        repositoryManager.updateRepositoryConfiguration(configuredRepository.getId(), new FileRepositoryConfiguration("/test"));
    }

    private void cleanup() {
        integrationTests.cleanup();
        variant = null;
        configuredRepository = null;
    }

    private Job getJob() {
        return Job.Builder.build(j -> {
            j.setJobType(JobType.INGEST);
            j.setJobConfiguration(getJobConfiguration());
        });
    }

    private JobConfiguration getJobConfiguration() {
        return IngestJobConfiguration.Builder.build(config -> {
            config.setVariant(variant.getId());
            config.setRepositories(ImmutableList.of(configuredRepository.getId()));
            config.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(ingestJobFile -> {
                ingestJobFile.setUri("file://test");
                ingestJobFile.setHash("a-b-c-d");
                ingestJobFile.setHashAlgorithm("MD5");
            })));
        });
    }
}
