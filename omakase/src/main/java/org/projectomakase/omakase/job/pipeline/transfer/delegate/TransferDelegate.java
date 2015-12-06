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
package org.projectomakase.omakase.job.pipeline.transfer.delegate;

import org.projectomakase.omakase.job.pipeline.transfer.Transfer;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.spi.TaskOutput;

/**
 * Transfer Delegate implementations expose a facade from managing the different types of transfers e.g. Ingest, Export, Replication.
 *
 * @author Richard Lucas
 */
public interface TransferDelegate {

    /**
     * Initiates a transfer file group.
     *
     * @param transferFileGroup
     *         the {@link TransferFileGroup} to initiate
     * @return the initiated {@link TransferFileGroup}
     */
    TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup);

    /**
     * Completes a transfer file group.
     *
     * @param transferFileGroup
     *         the {@link TransferFileGroup} to complete
     * @param taskOutput
     *         the output of the task that performed the transfer
     * @return the completed {@link TransferFileGroup}
     */
    TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput);

    /**
     * Aborts a transfer.
     *
     * @param transferFileGroup
     *         the {@link TransferFileGroup} to abort
     */
    void abortTransferFileGroup(TransferFileGroup transferFileGroup);

    /**
     * Updates the content repository.
     *
     * @param pipelineContext
     *         the pipeline context
     * @param transfer
     *         the transfer
     */
    void updateContentRepository(PipelineContext pipelineContext, Transfer transfer);

    /**
     * Cleans up the content repository.
     *
     * @param pipelineContext
     *         the pipeline context
     */
    void cleanupContentRepository(PipelineContext pipelineContext);


    /**
     * Creates the task that will perform the transfer.
     *
     * @param pipelineContext
     *         the pipeline context
     * @param transferFileGroup
     *         the transferFileGroup
     * @param taskGroup
     *         the task group the task belongs to
     * @return the created task.
     */
    Task createTask(PipelineContext pipelineContext, TransferFileGroup transferFileGroup, TaskGroup taskGroup);

}
