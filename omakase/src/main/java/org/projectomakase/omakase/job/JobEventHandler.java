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

import org.projectomakase.omakase.event.DeleteAsset;
import org.projectomakase.omakase.event.DeleteVariant;
import org.projectomakase.omakase.event.DeleteVariantFromRepository;
import org.projectomakase.omakase.event.SubmitJob;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.jboss.logging.Logger;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.function.Predicate;

/**
 * Implements observer methods for the different types of application events that need to be processed by the job framework.
 *
 * @author Richard Lucas
 */
public class JobEventHandler {

    private static final Logger LOGGER = Logger.getLogger(JobEventHandler.class);

    @Inject
    JobDAO jobDAO;

    /**
     * Observers delete asset events and throws an exception if the asset has any active jobs.
     *
     * @param deleteAsset
     *         a delete asset event
     * @throws NotUpdateableException
     *         if the asset has active jobs.
     */
    public void handleDeleteAsset(@Observes DeleteAsset deleteAsset) {
        jobDAO.findJobsForAsset(deleteAsset.getAssetId()).stream().findAny().filter(activeJobPredicate())
                .ifPresent(job -> throwNotUpdateableException("One or more active jobs are associated with asset " + deleteAsset.getAssetId()));
    }

    /**
     * Observers delete variant events and throws an exception if the variant has any active jobs.
     *
     * @param deleteVariant
     *         a delete variant event
     * @throws NotUpdateableException
     *         if the variant has active jobs.
     */
    public void handleDeleteVariant(@Observes DeleteVariant deleteVariant) {
        jobDAO.findJobsForVariant(deleteVariant.getVariantId()).stream().filter(activeJobPredicate()).findAny()
                .ifPresent(job -> throwNotUpdateableException("One or more active jobs are associated with variant " + deleteVariant.getVariantId()));
    }

    /**
     * Observers delete variant from repository events and throws an exception if the variant has any active jobs using the repository.
     *
     * @param deleteVariantFromRepository
     *         a delete variant from repository event
     * @throws NotUpdateableException
     *         if the variant has active jobs using the repository.
     */
    public void handleDeleteVariantFromRepository(@Observes DeleteVariantFromRepository deleteVariantFromRepository) {
        jobDAO.findJobsForVariantAndRepository(deleteVariantFromRepository.getVariantId(), deleteVariantFromRepository.getRepositoryId()).stream().filter(activeJobPredicate()).findAny().ifPresent(
                job -> throwNotUpdateableException(
                        "One or more active jobs are associated with variant " + deleteVariantFromRepository.getVariantId() + " and repository " + deleteVariantFromRepository.getRepositoryId()));
    }

    /**
     * Observes submit job events and throws an exception if the variant associated to the job has any active delete jobs.
     *
     * @param submitJob
     *         a submit job event
     * @throws NotUpdateableException
     *         if the variant associated to the job has any active delete jobs.
     */
    public void handleSubmitJob(@Observes SubmitJob submitJob) {
        jobDAO.findJobsForVariant(submitJob.getVariantId()).stream().filter(activeDeleteJobPredicate()).findAny()
                .ifPresent(job -> throwNotUpdateableException("One or more active delete jobs are associated with variant " + submitJob.getVariantId()));
    }

    private static Predicate<Job> activeJobPredicate() {
        return job -> JobStatus.QUEUED.equals(job.getStatus()) || JobStatus.EXECUTING.equals(job.getStatus());
    }

    private static Predicate<Job> activeDeleteJobPredicate() {
        return job -> JobType.DELETE.equals(job.getJobType()) && (JobStatus.QUEUED.equals(job.getStatus()) || JobStatus.EXECUTING.equals(job.getStatus()));
    }

    private static void throwNotUpdateableException(String message) {
        LOGGER.error(message);
        throw new NotUpdateableException(message);
    }
}
