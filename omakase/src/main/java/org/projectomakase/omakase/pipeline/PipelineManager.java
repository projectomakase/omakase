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
package org.projectomakase.omakase.pipeline;

import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.callback.Callback;
import org.projectomakase.omakase.jcr.OrganizationNodePath;
import org.jboss.logging.Logger;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * Pipeline Manager Java Facade
 * <p>
 * The pipeline manager is used to execute pipeline pipelines. A pipeline pipeline consists of one or more pipeline stages. A stage can be either synchronous or asynchronous. Synchronous stages are
 * executed by the current executing thread. Asynchronous stages are prepared by the current executing thread and then delegated to a background process such as the Omakase task framework, the
 * pipeline is halted while waiting for the asynchronous execution to complete. Execution of the pipeline resumes when the pipeline receives a callback event from the background process.
 * </p>
 *
 * @author Richard Lucas
 */
@Stateless
public class PipelineManager {

    private static final Logger LOGGER = Logger.getLogger(PipelineManager.class);

    @Inject
    @OrganizationNodePath()
    String organizationNodePath;
    @Inject
    PipelineDAO pipelineDAO;
    @Inject
    Callback callback;
    @Inject
    IdGenerator idGenerator;
    @Inject
    Event<Pipeline> pipelineEvent;

    /**
     * Executes the pipeline. Each pipeline stage is prepared in order until either the pipeline completes, fails or an asynchronous stage is prepared.
     * If the pipeline completes or fails the pipeline terminates. If an asynchronous stage is prepared the execution of the pipeline stops and waits for a
     * notification that the asynchronous stage has completed before continuing.
     * <p>
     * Synchronous stages are executed on the calling thread and are expected to return within a few milliseconds, if a stage takes longer than that to prepare it should be
     * changed to a asynchronous stage and executed as a background process.
     * </p>
     *
     * @param pipeline
     *         the pipeline to execute.
     */
    public void execute(Pipeline pipeline) {
        pipeline.setId(idGenerator.getId());
        Pipeline createdPipeline = pipelineDAO.create(getPipelineRootPath(pipeline), pipeline);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing " + pipeline);
        }

        // pipeline creation is done in the callers transaction. CDI eventing is then used to process the pipeline in it's own transaction after the callers transaction has completed.
        pipelineEvent.fire(createdPipeline);
    }

    private String getPipelineRootPath(Pipeline pipeline) {
        return pipelineDAO.getDistributedNodePath(organizationNodePath + "/" + pipeline.getObject(), pipeline.getObjectId());
    }
}
