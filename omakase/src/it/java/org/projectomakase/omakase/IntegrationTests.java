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
package org.projectomakase.omakase;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.broker.BrokerManager;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.content.Asset;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.job.task.queue.TaskQueue;
import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.search.DefaultSearchBuilder;
import org.projectomakase.omakase.search.Search;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.jcr.Session;
import javax.transaction.UserTransaction;

/**
 * A collection of methods used by the Integration Tests.
 *
 * @author Richard Lucas
 */
public class IntegrationTests {

    @Inject
    ContentManager contentManager;
    @Inject
    JobManager jobManager;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    LocationManager locationManager;
    @Inject
    BrokerManager brokerManager;
    @Resource
    UserTransaction userTransaction;
    @Inject
    TaskQueue taskQueue;
    @Inject
    Session session;
    @Inject
    IdGenerator idGenerator;
    @Inject
    BeanManager beanManager;

    public void cleanup() {
        Search search = new DefaultSearchBuilder().count(-1).build();
        brokerManager.findWorkers(search).getRecords().stream().map(Worker::getId).forEach(brokerManager::unregisterWorker);
        repositoryManager.findRepositories(search).getRecords().stream().map(Repository::getId).forEach(repoId -> repositoryManager.deleteRepository(repoId, true));
        locationManager.findLocations(search).getRecords().stream().map(Location::getId).forEach(locationId -> locationManager.deleteLocation(locationId));
        jobManager.findJobs(search).getRecords().stream().map(Job::getId).forEach(jobId -> jobManager.deleteJob(jobId, true));
        contentManager.findAssets(search).getRecords().stream().map(Asset::getId).forEach(assetId -> contentManager.deleteAsset(assetId, true));
        drainQueues();
    }

    public String createJob() {
        Asset asset = contentManager.createAsset(new Asset());
        Variant variant = contentManager.createVariant(asset.getId(), new Variant());
        Repository repository = repositoryManager.createRepository(new Repository(idGenerator.getId(), "", "FILE"));
        repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test"));
        Job job = jobManager.createJob(Job.Builder.build(j -> {
            j.setJobType(JobType.INGEST);
            j.setJobConfiguration(IngestJobConfiguration.Builder.build(config -> {
                config.setVariant(variant.getId());
                config.setRepositories(ImmutableList.of(repository.getId()));
                config.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(ingestJobFile -> ingestJobFile.setUri("file:/test"))));
            }));
        }));
        return job.getId();
    }

    public void drainQueues() {
        taskQueue.drain();
    }

    public void destroyJCRSession() {
        final AlterableContext requestContext = (AlterableContext) beanManager.getContext(RequestScoped.class);
        beanManager.getBeans(Session.class).stream().filter(bean -> bean != null).forEach(requestContext::destroy);
    }
}
