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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.callback.Callback;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.callback.CallbackListener;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStageExecutor;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;
import org.projectomakase.omakase.security.OmakaseSecurity;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Optional;

/**
 * Executes a pipeline.
 * <p>
 * Methods on this class should NOT be invoked directly, they are intended to be invoked via CDI Events. All direct interactions with the Pipeline Framework should be via the {@link
 * PipelineManager}.
 * </p>
 * <p>
 * The implementation relies on bean managed transactions in order to correctly isolate the different stages of the pipeline.
 * </p>
 * <p>
 * The following steps are involved in executing a pipeline:
 * </p>
 * <ol>
 * <li>Execute the first stage in it's own transaction</li>
 * <li>Update the pipeline with the result of the previous execution in it's own transaction</li>
 * <li>Check to see if the pipeline has additional stages and that the current stage is completed</li>
 * <li>Transition the pipeline to it's next stage and reset the stage's status, this is done in it's own transaction</li>
 * <li>Repeat for each stage until either the pipeline completes, fails or halts while executing a asynchronous process</li>
 * <li>Resume executing upon receiving a callback and invoke the stages callback handler in it's own transaction</li>
 * <li>Continue executing each stage as above until either the pipeline completes, fails or halts again</li>
 * <li>Repeat until the pipeline is moved to a terminating status</li>
 * </ol>
 * <p>
 * If an error occurs processing a stage it is rolled back and the pipeline's status is set to failed, this is then propagated to the pipeline's object. If an error occurs attempting to update the
 * pipeline or it's object then a OmakaseRuntimeException is thrown and the pipeline's processing is terminated and the error logged. This may result in the pipeline being left stuck with a status of
 * QUEUED or EXECUTING and will require manual intervention.
 * </p>
 *
 * @author Richard Lucas
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class PipelineExecutor {

    public static final String CALLBACK_LISTENER_ID = "PIPELINE_PROCESSOR";

    private static final Logger LOGGER = Logger.getLogger(PipelineExecutor.class);

    @Inject
    PipelineStageExecutor pipelineStageExecutor;
    @Inject
    PipelineDAO pipelineDAO;
    @Inject
    Callback callback;
    @Resource
    UserTransaction userTransaction;

    /**
     * Executes a pipeline, executing each stage in turn until the pipeline completes, fails or halts while waiting for an asynchronous process to complete.
     * <p>
     * Only pipelines that are in their initial state can be executed, if the pipeline is not in it's initial state it is marked as failed.
     * </p>
     *
     * @param pipeline
     *         the pipeline
     */
    @Asynchronous
    public void executePipeline(@Observes(during = TransactionPhase.AFTER_SUCCESS) Pipeline pipeline) {
        Throwables.voidInstance(() -> OmakaseSecurity.doAsSystem(() -> {
            if (!pipeline.isFirstPipelineStage() && !PipelineStageStatus.QUEUED.equals(pipeline.getStatusOfCurrentStage())) {
                LOGGER.error("Pipeline " + pipeline.getId() + " is not in it's initial state");
                updatePipeline(PipelineStageResult.builder(pipeline.getId(), PipelineStageStatus.FAILED).addMessages(ImmutableSet.of("The pipeline is not in it's initial state")).build());
            }

            // the first stage is executed outside of recursion as, the recursion relies on the the previous stage
            // being complete before executing the next one which is not the case for the first stage.
            PipelineContext pipelineContext = createPipelineContext(pipeline);
            PipelineStageResult pipelineStageResult = pipelineStageExecutor.execute(pipelineContext, pipeline.getCurrentPipelineStage());

            executePipelineFailureStage(pipelineStageResult);

            Pipeline updatedPipeline = updatePipeline(pipelineStageResult);
            // execute subsequent stages until the pipeline is halted or completed.
            executePipelineStages(updatedPipeline);
            return true;
        }));

    }

    /**
     * Resumes processing of a pipeline upon receiving a callback.
     *
     * @param callbackEvent
     *         the callback event
     */
    public void onCallback(@Observes @CallbackListener(CALLBACK_LISTENER_ID) CallbackEvent callbackEvent) {

        Optional<Pipeline> pipeline = pipelineDAO.findById(callbackEvent.getObjectId());

        if (pipeline.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("resuming " + pipeline.get());
            }

            // handle callback
            PipelineContext pipelineContext = createPipelineContext(pipeline.get());
            PipelineStageResult pipelineStageResult = pipelineStageExecutor.onCallback(pipelineContext, pipeline.get().getCurrentPipelineStage(), callbackEvent);

            executePipelineFailureStage(pipelineStageResult);

            Pipeline updatedPipeline = updatePipeline(pipelineStageResult);

            // execute subsequent stages
            executePipelineStages(updatedPipeline);

        } else {
            LOGGER.error("Unable to find pipeline " + callbackEvent.getObjectId());
        }

    }

    private void executePipelineFailureStage(PipelineStageResult pipelineStageResult) {
        if (PipelineStageStatus.FAILED.equals(pipelineStageResult.getPipelineStageStatus())) {
            Pipeline failedPipeline = updateFailedPipeline(pipelineStageResult);
            failedPipeline.getFailureStage().ifPresent(failureStage -> pipelineStageExecutor.onFailure(createPipelineContext(failedPipeline), failureStage));
        }
    }

    private void executePipelineStages(Pipeline pipeline) {
        Pipeline updatedPipeline = pipeline;
        while (!updatedPipeline.isLastPipelineStage()) {
            if (PipelineStageStatus.COMPLETED.equals(updatedPipeline.getStatusOfCurrentStage())) {

                // Tx1
                updatedPipeline = nextPipelineStage(updatedPipeline);

                // Tx2
                PipelineContext pipelineContext = createPipelineContext(updatedPipeline);
                PipelineStageResult pipelineStageResult = pipelineStageExecutor.execute(pipelineContext, updatedPipeline.getCurrentPipelineStage());

                // Tx3
                updatedPipeline = updatePipeline(pipelineStageResult);
            }else {
                break;
            }
        }
    }

    private Pipeline nextPipelineStage(Pipeline pipeline) {

        Throwables.voidInstance(userTransaction::begin);

        pipeline.setStatusOfCurrentStage(PipelineStageStatus.QUEUED);
        pipeline.incrementCurrentStage();
        Pipeline updatedPipeline = pipelineDAO.update(pipeline);

        Throwables.voidInstance(userTransaction::commit);
        return updatedPipeline;
    }

    private Pipeline updatePipeline(PipelineStageResult pipelineStageResult) {

        try {
            // Pipeline updates need to be done in their own transaction.
            Throwables.voidInstance(userTransaction::begin);

            Pipeline pipeline =
                    pipelineDAO.findById(pipelineStageResult.getPipelineId()).orElseThrow(() -> new OmakaseRuntimeException("Pipeline " + pipelineStageResult.getPipelineId() + " does not exist"));

            pipeline.setStatusOfCurrentStage(pipelineStageResult.getPipelineStageStatus());
            pipeline.setStatus(getNextPipelineStatus(pipeline));
            pipeline.getProperties().putAll(pipelineStageResult.getProperties());
            pipelineStageResult.getAdditionalPipelineStages().forEach(pipeline::addNewPipelineStageAfterCurrentStage);
            Pipeline updatedPipeline = pipelineDAO.update(pipeline);

            ImmutableMultimap.Builder<String, String> callbackPropertiesBuilder = ImmutableMultimap.builder();
            callbackPropertiesBuilder.put("status", updatedPipeline.getStatus().name());
            pipelineStageResult.getMessages().forEach(message -> callbackPropertiesBuilder.put("message", message));
            callback.fire(new CallbackEvent(updatedPipeline.getObjectId(), callbackPropertiesBuilder.build()), updatedPipeline.getCallbackListenerId());

            Throwables.voidInstance(userTransaction::commit);

            return updatedPipeline;
        } catch (OmakaseRuntimeException e) {
            Throwables.voidInstance(userTransaction::rollback);

            String message = "Failed to update pipeline " + pipelineStageResult.getPipelineId() + ". Reason: " + e.getMessage();
            LOGGER.error(message, e);
            throw e;
        } catch (Exception e) {
            Throwables.voidInstance(userTransaction::rollback);

            String message = "Failed to update pipeline " + pipelineStageResult.getPipelineId() + ". Reason: " + e.getMessage();
            LOGGER.error(message, e);
            throw new OmakaseRuntimeException(message, e);
        }
    }

    private Pipeline updateFailedPipeline(PipelineStageResult pipelineStageResult) {

        try {
            // Pipeline updates need to be done in their own transaction.
            Throwables.voidInstance(userTransaction::begin);

            Pipeline pipeline =
                    pipelineDAO.findById(pipelineStageResult.getPipelineId()).orElseThrow(() -> new OmakaseRuntimeException("Pipeline " + pipelineStageResult.getPipelineId() + " does not exist"));
            pipeline.getProperties().putAll(pipelineStageResult.getProperties());
            Pipeline updatedPipeline = pipelineDAO.update(pipeline);

            Throwables.voidInstance(userTransaction::commit);

            return updatedPipeline;
        } catch (OmakaseRuntimeException e) {
            Throwables.voidInstance(userTransaction::rollback);

            String message = "Failed to update pipeline " + pipelineStageResult.getPipelineId() + ". Reason: " + e.getMessage();
            LOGGER.error(message, e);
            throw e;
        } catch (Exception e) {
            Throwables.voidInstance(userTransaction::rollback);

            String message = "Failed to update pipeline " + pipelineStageResult.getPipelineId() + ". Reason: " + e.getMessage();
            LOGGER.error(message, e);
            throw new OmakaseRuntimeException(message, e);
        }
    }

    private static PipelineStatus getNextPipelineStatus(Pipeline pipeline) {
        switch (pipeline.getStatus()) {
            case QUEUED:
                return getNextPipelineStatusQueued(pipeline);
            case EXECUTING:
                return getNextPipelineStatusExecuting(pipeline);
            default:
                throw new OmakaseRuntimeException("Unable to get new Pipeline Status. Pipeline " + pipeline.getId() + " is in terminating status " + pipeline.getStatus());
        }
    }

    private static PipelineStatus getNextPipelineStatusQueued(Pipeline pipeline) {
        PipelineStatus pipelineStatus;
        switch (pipeline.getStatusOfCurrentStage()) {
            case QUEUED:
                pipelineStatus = PipelineStatus.QUEUED;
                break;
            case EXECUTING:
                pipelineStatus = PipelineStatus.EXECUTING;
                break;
            case COMPLETED:
                pipelineStatus = getPipelineStatusForCompletedStage(pipeline);
                break;
            case FAILED:
                pipelineStatus = PipelineStatus.FAILED;
                break;
            default:
                throw new OmakaseRuntimeException("Unexpected pipeline stage status " + pipeline.getStatusOfCurrentStage());
        }
        return pipelineStatus;
    }

    private static PipelineStatus getNextPipelineStatusExecuting(Pipeline pipeline) {
        PipelineStatus pipelineStatus;
        switch (pipeline.getStatusOfCurrentStage()) {
            case QUEUED:
            case EXECUTING:
                pipelineStatus = PipelineStatus.EXECUTING;
                break;
            case COMPLETED:
                pipelineStatus = getPipelineStatusForCompletedStage(pipeline);
                break;
            case FAILED:
                pipelineStatus = PipelineStatus.FAILED;
                break;
            default:
                throw new OmakaseRuntimeException("Unexpected pipeline stage status " + pipeline.getStatusOfCurrentStage());
        }
        return pipelineStatus;
    }

    private static PipelineStatus getPipelineStatusForCompletedStage(Pipeline pipeline) {
        if (pipeline.isLastPipelineStage()) {
            return PipelineStatus.COMPLETED;
        } else {
            return PipelineStatus.EXECUTING;
        }
    }

    private static PipelineContext createPipelineContext(Pipeline pipeline) {
        return new PipelineContext(pipeline.getId(), pipeline.getObjectId(), pipeline.getObject(), pipeline.getCallbackListenerId(), ImmutableMap.copyOf(pipeline.getProperties()));
    }
}
