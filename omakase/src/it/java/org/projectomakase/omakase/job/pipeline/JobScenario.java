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

import org.projectomakase.omakase.job.JobType;

import java.util.ArrayList;
import java.util.List;

/**
 * A Job Scenario can be used to setup and test a Job Pipeline to ensure it behaves correctly for the given scenario e.g. a successful ingest or a failed export.
 *
 * @author Richard Lucas
 */
public class JobScenario {

    private final List<RepositoryInfo> repositoryInfos;
    private final List<JobInfo> jobInfos;

    public JobScenario(List<RepositoryInfo> repositoryInfos, List<JobInfo> jobInfos) {
        this.repositoryInfos = repositoryInfos;
        this.jobInfos = jobInfos;
    }

    public List<RepositoryInfo> getRepositoryInfos() {
        return repositoryInfos;
    }

    public List<JobInfo> getJobInfos() {
        return jobInfos;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<RepositoryInfo> repositoryInfos = new ArrayList<>();
        private final List<JobInfo> jobInfos = new ArrayList<>();

        public Builder repository(String name, String type) {
            repositoryInfos.add(new RepositoryInfo(name, type));
            return this;
        }

        public Builder ingestJob(String repositoryName, int numberOfFiles) {
            jobInfos.add(new IngestJobInfo(JobFailureType.NONE, repositoryName, numberOfFiles));
            return this;
        }

        public Builder hlsIngestJob(String repositoryName, HLSManifestType hlsManifestType) {
            jobInfos.add(new HLSIngestJobInfo(JobFailureType.NONE, repositoryName, hlsManifestType));
            return this;
        }

        public Builder failedIngestJob(String repositoryName, int numberOfFiles, JobFailureType jobFailureType) {
            jobInfos.add(new IngestJobInfo(jobFailureType, repositoryName, numberOfFiles));
            return this;
        }

        public Builder exportJob(String repositoryName) {
            jobInfos.add(new ExportJobInfo(JobFailureType.NONE, repositoryName));
            return this;
        }

        public Builder hlsExportJob(String repositoryName) {
            jobInfos.add(new HLSExportJobInfo(JobFailureType.NONE, repositoryName));
            return this;
        }

        public Builder failedExportJob(String repositoryName, JobFailureType jobFailureType) {
            jobInfos.add(new ExportJobInfo(jobFailureType, repositoryName));
            return this;
        }

        public Builder replicationJob(String sourceRepositoryName, String destinationRepositoryId) {
            jobInfos.add(new ReplicationJobInfo(JobFailureType.NONE, sourceRepositoryName, destinationRepositoryId));
            return this;
        }

        public Builder hlsReplicationJob(String sourceRepositoryName, String destinationRepositoryId) {
            jobInfos.add(new HLSReplicationJobInfo(JobFailureType.NONE, sourceRepositoryName, destinationRepositoryId));
            return this;
        }

        public Builder failedReplicationJob(String sourceRepositoryName, String destinationRepositoryId, JobFailureType jobFailureType) {
            jobInfos.add(new ReplicationJobInfo(jobFailureType, sourceRepositoryName, destinationRepositoryId));
            return this;
        }

        public Builder deleteJob() {
            jobInfos.add(new DeleteJobInfo(JobType.DELETE, JobFailureType.NONE));
            return this;
        }

        public JobScenario build() {
            return new JobScenario(repositoryInfos, jobInfos);
        }
    }

    static class RepositoryInfo {
        private final String name;
        private final String type;

        public RepositoryInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    static class IngestJobInfo extends JobInfo{
        private final String repositoryName;
        private final int numberOfFiles;

        public IngestJobInfo(JobFailureType jobFailureType, String repositoryName, int numberOfFiles) {
            super(jobFailureType);
            this.repositoryName = repositoryName;
            this.numberOfFiles = numberOfFiles;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public int getNumberOfFiles() {
            return numberOfFiles;
        }

    }

    static class HLSIngestJobInfo extends JobInfo{
        private final String repositoryName;
        private final HLSManifestType hlsManifestType;

        public HLSIngestJobInfo(JobFailureType jobFailureType, String repositoryName, HLSManifestType hlsManifestType) {
            super(jobFailureType);
            this.repositoryName = repositoryName;
            this.hlsManifestType = hlsManifestType;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public HLSManifestType getHlsManifestType() {
            return hlsManifestType;
        }
    }

    static class ExportJobInfo extends JobInfo {
        private final String repositoryName;

        public ExportJobInfo(JobFailureType jobFailureType, String repositoryName) {
            super(jobFailureType);
            this.repositoryName = repositoryName;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

    }

    static class HLSExportJobInfo extends JobInfo {
        private final String repositoryName;

        public HLSExportJobInfo(JobFailureType jobFailureType, String repositoryName) {
            super(jobFailureType);
            this.repositoryName = repositoryName;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

    }

    static class ReplicationJobInfo extends JobInfo {
        private final String sourceRepositoryName;
        private final String destinationRepositoryId;

        public ReplicationJobInfo(JobFailureType jobFailureType, String sourceRepositoryName, String destinationRepositoryId) {
            super(jobFailureType);
            this.sourceRepositoryName = sourceRepositoryName;
            this.destinationRepositoryId = destinationRepositoryId;
        }

        public String getSourceRepositoryName() {
            return sourceRepositoryName;
        }

        public String getDestinationRepositoryName() {
            return destinationRepositoryId;
        }
    }

    static class HLSReplicationJobInfo extends JobInfo {
        private final String sourceRepositoryName;
        private final String destinationRepositoryId;

        public HLSReplicationJobInfo(JobFailureType jobFailureType, String sourceRepositoryName, String destinationRepositoryId) {
            super(jobFailureType);
            this.sourceRepositoryName = sourceRepositoryName;
            this.destinationRepositoryId = destinationRepositoryId;
        }

        public String getSourceRepositoryName() {
            return sourceRepositoryName;
        }

        public String getDestinationRepositoryName() {
            return destinationRepositoryId;
        }
    }

    static class DeleteJobInfo extends JobInfo {

        public DeleteJobInfo(JobType jobType, JobFailureType jobFailureType) {
            super(jobFailureType);
        }
    }

    static class JobInfo {
        private final JobFailureType jobFailureType;

        public JobInfo(JobFailureType jobFailureType) {
            this.jobFailureType = jobFailureType;
        }

        public JobFailureType getJobFailureType() {
            return jobFailureType;
        }
    }

    enum JobFailureType {
        NONE, MULTIPART_PREPARE, RESTORE, TRANSFER, EMPTY_REPOSITORY
    }

    enum HLSManifestType {
        MEDIA, MASTER
    }
}
