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
import com.google.common.primitives.Ints;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.content.VariantFileSearchBuilder;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.task.api.TaskStatus;
import org.projectomakase.omakase.task.api.TaskStatusUpdate;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class DeleteDelegate {

    @Inject
    JobManager jobManager;
    @Inject
    ContentManager contentManager;
    @Inject
    JobScenarioTools jobScenarioTools;


    public void execute(JobScenario.DeleteJobInfo deleteJobInfo, Worker worker, Asset asset, Variant variant) {
        int numberOfFiles = Ints.checkedCast(contentManager.findVariantFiles(variant.getId(), new VariantFileSearchBuilder().build()).getTotalRecords());
        String jobId = contentManager.deleteVariant(asset.getId(), variant.getId()).get();

        jobScenarioTools.waitUntilJobsHasTaskGroupWithStatus(jobId, TaskStatus.QUEUED);

        if (JobStatus.QUEUED.equals(getJobStatus(jobId))) {
            jobScenarioTools.consumeTasks(worker, 1, "DELETE", ImmutableList.of(new TaskStatusUpdate(TaskStatus.COMPLETED, "Success", 100)));
            jobScenarioTools.waitUntilJobHasStatus(jobId, JobStatus.COMPLETED);
        }
        assertThat(getJobStatus(jobId)).isEqualTo(JobStatus.COMPLETED);

    }

    private JobStatus getJobStatus(String jobId) {
        return jobManager.getJob(jobId).get().getStatus();
    }
}
