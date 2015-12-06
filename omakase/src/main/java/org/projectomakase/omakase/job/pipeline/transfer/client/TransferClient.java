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
package org.projectomakase.omakase.job.pipeline.transfer.client;

import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.spi.TaskOutput;

/**
 * Transfer client implementations expose a facade for managing transfers via a a service specific client e.g. AWS S3
 *
 * @author Richard Lucas
 */
public interface TransferClient {

    /**
     * Initiates a transfer file group.
     *
     * @param transferFileGroup
     *         the {@link TransferFileGroup} to initiate
     * @return the initiated {@link TransferFile}
     */
    TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup);

    /**
     * Completes a transfer file group.
     *
     * @param transferFileGroup
     *         the {@link TransferFileGroup} to initiate
     * @param taskOutput
     *         the output of the task that performed the transfer
     * @return the completed {@link TransferFileGroup}
     */
    TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput);

    /**
     * Aborts a transfer file group.
     *
     * @param transferFileGroup
     *         the {@link TransferFileGroup} to abort
     */
    void abortTransferFileGroup(TransferFileGroup transferFileGroup);

    /**
     * Creates a service specific task e.g. S3_UPLOAD task that will perform the transfer.
     *
     * @param transferFileGroup
     *         the transferFileGroup
     * @param taskGroup
     *         the task group the task belongs to
     * @param description
     *         a task description
     * @param priority
     *         the tasks priority
     * @return the created task.
     */
    Task createTask(TransferFileGroup transferFileGroup, TaskGroup taskGroup, String description, int priority);
}
