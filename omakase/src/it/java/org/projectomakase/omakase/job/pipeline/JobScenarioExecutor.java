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
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.repository.provider.glacier.GlacierRepositoryConfiguration;
import org.projectomakase.omakase.repository.provider.s3.S3RepositoryConfiguration;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Richard Lucas
 */
public class JobScenarioExecutor {

    private static final Logger LOGGER = Logger.getLogger(JobScenarioExecutor.class);

    @Inject
    RepositoryManager repositoryManager;
    @Inject
    ContentManager contentManager;
    @Inject
    BrokerManager brokerManager;
    @Inject
    IngestDelegate ingestDelegate;
    @Inject
    HLSIngestDelegate hlsIngestDelegate;
    @Inject
    ExportDelegate exportDelegate;
    @Inject
    HLSExportDelegate hlsExportDelegate;
    @Inject
    ReplicationDelegate replicationDelegate;
    @Inject
    HLSReplicationDelegate hlsReplicationDelegate;
    @Inject
    DeleteDelegate deleteDelegate;

    public void execute(JobScenario jobScenario) {
        List<Repository> repositories = jobScenario.getRepositoryInfos().stream().map(this::createRepository).collect(ImmutableListCollector.toImmutableList());

        Asset asset = contentManager.createAsset(new Asset("asset", ImmutableList.of()));
        Variant variant = contentManager.createVariant(asset.getId(), new Variant("variant", ImmutableList.of()));

        Worker worker = brokerManager.registerWorker(Worker.Builder.build(w -> w.setWorkerName("worker1")));

        jobScenario.getJobInfos().forEach(jobInfo -> {
            if (jobInfo instanceof JobScenario.IngestJobInfo) {
                ingestDelegate.execute((JobScenario.IngestJobInfo) jobInfo, worker, asset, variant);
            } else if (jobInfo instanceof JobScenario.HLSIngestJobInfo) {
                hlsIngestDelegate.execute((JobScenario.HLSIngestJobInfo) jobInfo, worker, asset, variant);
            } else if (jobInfo instanceof JobScenario.ExportJobInfo) {
                exportDelegate.execute((JobScenario.ExportJobInfo) jobInfo, worker, variant);
            } else if (jobInfo instanceof JobScenario.HLSExportJobInfo) {
                hlsExportDelegate.execute((JobScenario.HLSExportJobInfo) jobInfo, worker, variant);
            } else if (jobInfo instanceof JobScenario.ReplicationJobInfo) {
                replicationDelegate.execute((JobScenario.ReplicationJobInfo) jobInfo, worker, asset, variant);
            } else if (jobInfo instanceof JobScenario.HLSReplicationJobInfo) {
                hlsReplicationDelegate.execute((JobScenario.HLSReplicationJobInfo) jobInfo, worker, variant);
            } else if (jobInfo instanceof JobScenario.DeleteJobInfo) {
                deleteDelegate.execute((JobScenario.DeleteJobInfo) jobInfo, worker, asset, variant);
            }
        });
    }

    private Repository createRepository(JobScenario.RepositoryInfo repositoryInfo) {
        Repository repository = repositoryManager.createRepository(new Repository(repositoryInfo.getName(), "", repositoryInfo.getType()));
        switch (repositoryInfo.getType()) {
            case "FILE":
                repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test/repository"));
                break;
            case "RESTORABLE-TEST":
                repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test/repository"));
                break;
            case "S3" :
                repositoryManager.updateRepositoryConfiguration(repository.getId(), new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", null));
                break;
            case "GLACIER":
                repositoryManager.updateRepositoryConfiguration(repository.getId(), new GlacierRepositoryConfiguration("access", "secret", "us-west-1", "test", "test"));
            default:
                break;
        }
        return repository;
    }

}
