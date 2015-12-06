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
package org.projectomakase.omakase.pipeline.stage;

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.api.provider.DependentProvider;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Pipeline Stage Executor.
 *
 * @author Richard Lucas
 */
@Stateless
public class PipelineStageExecutor {

    private static final Logger LOGGER = Logger.getLogger(PipelineStageExecutor.class);

    @Resource
    EJBContext ejbContext;

    /**
     * Executes the pipeline stage with the given pipeline context and returns the result.
     * <p>
     * Invokes the prepare method on the stage and returns the result. The stage is invoked in it's own transaction. If an exception is caught the transaction is rolled back and the stage is marked
     * as FAILED.
     * </p>
     *
     * @param pipelineContext
     *         the pipeline context
     * @param pipelineStage
     *         the pipeline stage class
     * @return a {@link PipelineStageResult}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public PipelineStageResult execute(PipelineContext pipelineContext, Class<PipelineStage> pipelineStage) {
        DependentProvider<PipelineStage> dependentProvider = null;
        try {
            logBefore(pipelineContext, pipelineStage, "preparing");
            dependentProvider = BeanProvider.getDependent(pipelineStage);
            PipelineStageResult result = dependentProvider.get().prepare(pipelineContext);
            logAfter(pipelineContext, pipelineStage, result);
            return result;
        } catch (Exception e) {
            ejbContext.setRollbackOnly();

            String message = "Pipeline execution failed at stage " + pipelineStage.getName() + ". Reason " + e.getMessage();
            LOGGER.error(message, e);
            return PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).addMessages(ImmutableSet.of(message)).build();
        } finally {
            if (dependentProvider != null) {
                dependentProvider.destroy();
            }
        }
    }

    /**
     * Executes the pipeline stage with the given pipeline context and callback event before returning the result.
     * <p>
     * Invoke the callback method on the stage and returns the result. The stage is invoked in it's own transaction. If an exception is caught the transaction is rolled back and the stage is marked
     * as FAILED.
     * </p>
     *
     * @param pipelineContext
     *         the pipeline context
     * @param pipelineStage
     *         the pipeline stage
     * @param callbackEvent
     *         the callback event
     * @return a {@link PipelineStageResult}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public PipelineStageResult onCallback(PipelineContext pipelineContext, Class<PipelineStage> pipelineStage, CallbackEvent callbackEvent) {
        DependentProvider<PipelineStage> dependentProvider = null;
        try {
            logBefore(pipelineContext, pipelineStage, "resuming");
            dependentProvider = BeanProvider.getDependent(pipelineStage);
            PipelineStageResult result = dependentProvider.get().onCallback(pipelineContext, callbackEvent);
            logAfter(pipelineContext, pipelineStage, result);
            return result;
        } catch (Exception e) {
            ejbContext.setRollbackOnly();

            String message = "Pipeline execution failed at stage " + pipelineStage.getName() + ". Reason " + e.getMessage();
            LOGGER.error(message, e);
            return PipelineStageResult.builder(pipelineContext.getPipelineId(), PipelineStageStatus.FAILED).addMessages(ImmutableSet.of(message)).build();
        } finally {
            if (dependentProvider != null) {
                dependentProvider.destroy();
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void onFailure(PipelineContext pipelineContext, Class<PipelineFailureStage> pipelineFailureStage) {
        DependentProvider<PipelineFailureStage> dependentProvider = null;
        try {
            LOGGER.debug("executing pipeline failure stage " + pipelineFailureStage.getName());
            dependentProvider = BeanProvider.getDependent(pipelineFailureStage);
            dependentProvider.get().onFailure(pipelineContext);
            LOGGER.debug("executed pipeline failure stage " + pipelineFailureStage.getName());
        } catch (Exception e) {
            ejbContext.setRollbackOnly();
            String message = "Failed to execute pipeline failure stage " + pipelineFailureStage.getName() + ". Reason " + e.getMessage();
            LOGGER.error(message, e);

        } finally {
            if (dependentProvider != null) {
                dependentProvider.destroy();
            }
        }
    }

    private static void logBefore(PipelineContext pipelineContext, Class<PipelineStage> pipelineStage, String action) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(action + " - " + pipelineStage.getSimpleName() + " for " + pipelineContext);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PipelineContext properties: " + pipelineContext.getProperties());
        }
    }

    private static void logAfter(PipelineContext pipelineContext, Class<PipelineStage> pipelineStage, PipelineStageResult result) {
        if (PipelineStageStatus.FAILED.equals(result.getPipelineStageStatus())) {
            LOGGER.error(result.getPipelineStageStatus().name().toLowerCase() + " - " + pipelineStage.getSimpleName() + " for " + pipelineContext);
        } else if (PipelineStageStatus.COMPLETED.equals(result.getPipelineStageStatus())) {
            LOGGER.info(result.getPipelineStageStatus().name().toLowerCase() + " - " + pipelineStage.getSimpleName() + " for " + pipelineContext);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("waiting for callback - " + pipelineStage.getSimpleName() + " for " + pipelineContext);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PipelineStageResult properties: " + result.getProperties());
        }
    }
}
