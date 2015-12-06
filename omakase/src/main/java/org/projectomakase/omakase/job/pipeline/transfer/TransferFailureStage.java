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
package org.projectomakase.omakase.job.pipeline.transfer;

import org.projectomakase.omakase.job.pipeline.transfer.delegate.TransferDelegateResolver;
import org.projectomakase.omakase.pipeline.Pipelines;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineFailureStage;
import org.jboss.logging.Logger;

import javax.inject.Inject;

/**
 * @author Richard Lucas
 */
public class TransferFailureStage implements PipelineFailureStage {

    private static final Logger LOGGER = Logger.getLogger(TransferFailureStage.class);

    @Inject
    TransferDelegateResolver transferDelegateResolver;

    @Override
    public void onFailure(PipelineContext pipelineContext) {
        try {
            transferDelegateResolver.resolve(Pipelines.getPipelineProperty(pipelineContext, "transferType")).cleanupContentRepository(pipelineContext);
        } catch (Exception e) {
            LOGGER.error("Failed to clean-up content repository. Reason: " + e.getMessage(), e);
        }
    }
}
