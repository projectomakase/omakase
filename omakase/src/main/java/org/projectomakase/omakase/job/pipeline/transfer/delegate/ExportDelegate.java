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

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.content.ContentManager;
import org.projectomakase.omakase.content.Variant;
import org.projectomakase.omakase.job.pipeline.transfer.Transfer;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.pipeline.transfer.client.TransferClient;
import org.projectomakase.omakase.job.pipeline.transfer.client.TransferClientResolver;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.pipeline.Pipelines;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.spi.TaskOutput;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Export {@link TransferDelegate} implementation.
 *
 * @author Richard Lucas
 */
public class ExportDelegate implements TransferDelegate {

    @Inject
    ContentManager contentManager;
    @Inject
    TaskManager taskManager;
    @Inject
    TransferClientResolver transferClientResolver;

    @Override
    public TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup) {
        return getTransferClient(transferFileGroup).initiateTransferFileGroup(transferFileGroup);
    }

    @Override
    public TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput) {
        return getTransferClient(transferFileGroup).completeTransferFileGroup(transferFileGroup, taskOutput);
    }

    @Override
    public void abortTransferFileGroup(TransferFileGroup transferFileGroup) {
        getTransferClient(transferFileGroup).abortTransferFileGroup(transferFileGroup);
    }

    @Override
    public Task createTask(PipelineContext pipelineContext, TransferFileGroup transferFileGroup, TaskGroup taskGroup) {
        int priority = Integer.parseInt(Pipelines.getPipelineProperty(pipelineContext, "priority"));
        String variantId = Pipelines.getPipelineProperty(pipelineContext, "variant");
        Variant variant = contentManager.getVariant(variantId).orElseThrow(() -> new OmakaseRuntimeException("Variant " + variantId + " does not exist"));
        String description = "Export Variant " + variant.getId() + Optional.ofNullable(variant.getVariantName()).map(name -> "(" + name + ")").orElse("");
        return getTransferClient(transferFileGroup).createTask(transferFileGroup, taskGroup, description, priority);
    }

    @Override
    public void updateContentRepository(PipelineContext pipelineContext, Transfer transfer) {
        // no-op
    }

    @Override
    public void cleanupContentRepository(PipelineContext pipelineContext) {
        // no-op
    }

    private TransferClient getTransferClient(TransferFileGroup transferFileGroup) {
        return transferClientResolver.resolve(transferFileGroup);
    }
}
