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

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a Omakase Job.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:job"})
public class Job extends JcrEntity {

    public static final String JOB_NAME = "omakase:name";
    public static final String EXTERNAL_IDS = "omakase:externalIds";
    public static final String TYPE = "omakase:type";
    public static final String STATUS = "omakase:status";
    public static final String STATUS_TIMESTAMP = "omakase:statusTimestamp";
    public static final String PRIORITY = "omakase:priority";
    public static final String SYSTEM = "omakase:system";

    private static final long DEFAULT_PRIORITY = 4;

    @JcrProperty(name = JOB_NAME)
    private String jobName;
    @JcrProperty(name = EXTERNAL_IDS)
    private List<String> externalIds;
    @JcrProperty(name = TYPE)
    private JobType jobType;
    @JcrProperty(name = STATUS)
    private JobStatus status = JobStatus.UNSUBMITTED;
    @JcrProperty(name = STATUS_TIMESTAMP)
    private Date statusTimestamp;
    @JcrProperty(name = PRIORITY)
    private long priority = DEFAULT_PRIORITY;
    @JcrProperty(name = SYSTEM)
    private boolean system = false;
    @JcrChildNode(createContainerNode = false)
    private JobConfiguration jobConfiguration;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public List<String> getExternalIds() {
        if (externalIds == null) {
            externalIds = new ArrayList<>();
        }
        return externalIds;
    }

    public void setExternalIds(List<String> externalIds) {
        this.externalIds = externalIds;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Date getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Date statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public JobConfiguration getJobConfiguration() {
        return jobConfiguration;
    }

    public void setJobConfiguration(JobConfiguration jobConfiguration) {
        this.jobConfiguration = jobConfiguration;
    }

    public static class Builder {
        @FunctionalInterface
        public interface JobSetter extends Consumer<Job> {
        }

        public static Job build(JobSetter... jobSetters) {
            final Job job = new Job();

            Stream.of(jobSetters).forEach(s -> s.accept(job));

            return job;
        }
    }
}
