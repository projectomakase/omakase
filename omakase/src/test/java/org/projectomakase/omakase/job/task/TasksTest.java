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

import org.projectomakase.omakase.task.api.TaskStatus;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author Richard Lucas
 */
public class TasksTest {

    private Tasks tasks;

    @Before
    public void setUp() throws Exception {
        tasks = new Tasks();
    }

    @Test
    public void shouldReturnTrueForIsFailedTaskStatus() throws Exception {
        assertThat(tasks.isFailedTaskStatus(TaskStatus.FAILED_CLEAN)).isTrue();
        assertThat(tasks.isFailedTaskStatus(TaskStatus.FAILED_DIRTY)).isTrue();
    }

    @Test
    public void shouldReturnFalseForIsFailedTaskStatus() throws Exception {
        assertThat(tasks.isFailedTaskStatus(TaskStatus.QUEUED)).isFalse();
        assertThat(tasks.isFailedTaskStatus(TaskStatus.EXECUTING)).isFalse();
        assertThat(tasks.isFailedTaskStatus(TaskStatus.COMPLETED)).isFalse();
    }

    @Test
    public void shouldGetMaxTaskRetries() throws Exception {
        tasks.maxTaskRetries = 3;
        assertThat(tasks.getMaxTaskRetries()).isEqualTo(3);
    }

    @Test
    public void shouldGetMaxTaskRetriesWhenLessThanZero() throws Exception {
        tasks.maxTaskRetries = -10;
        assertThat(tasks.getMaxTaskRetries()).isEqualTo(0);
    }

    @Test
    public void shouldGetMaxTaskRetriesWhenGreaterThanTen() throws Exception {
        tasks.maxTaskRetries = 100;
        assertThat(tasks.getMaxTaskRetries()).isEqualTo(10);
    }

    @Test
    public void shouldRetryFailedTask() throws Exception {
        assertThat(tasks.shouldRetryTask(TaskStatus.FAILED_CLEAN, 0, 1)).isTrue();
        assertThat(tasks.shouldRetryTask(TaskStatus.FAILED_DIRTY, 0, 1)).isTrue();
    }

    @Test
    public void shouldNotRetryNonFailedTask() throws Exception {
        assertThat(tasks.shouldRetryTask(TaskStatus.QUEUED, 0, 1)).isFalse();
        assertThat(tasks.shouldRetryTask(TaskStatus.EXECUTING, 0, 1)).isFalse();
        assertThat(tasks.shouldRetryTask(TaskStatus.COMPLETED, 0, 1)).isFalse();
    }
}