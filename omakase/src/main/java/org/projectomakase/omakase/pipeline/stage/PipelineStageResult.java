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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * The result of executing prepare or onCallback on a pipeline stage.
 *
 * @author Richard Lucas
 */
public final class PipelineStageResult {

    private final String pipelineId;
    private final PipelineStageStatus pipelineStageStatus;
    private final ImmutableMap<String, String> properties;
    private final ImmutableSet<String> messages;
    private final ImmutableSet<Class<? extends PipelineStage>> additionalPipelineStages;

    private PipelineStageResult(String pipelineId, PipelineStageStatus pipelineStageStatus, ImmutableMap<String, String> properties, ImmutableSet<String> messages,
                                ImmutableSet<Class<? extends PipelineStage>> additionalPipelineStages) {
        this.pipelineId = pipelineId;
        this.pipelineStageStatus = pipelineStageStatus;
        this.properties = properties;
        this.messages = messages;
        this.additionalPipelineStages = additionalPipelineStages;
    }

    /**
     * Returns the id of the pipeline being processed by the stage.
     *
     * @return the id of the pipeline being processed by the stage.
     */
    public String getPipelineId() {
        return pipelineId;
    }

    /**
     * Returns the result of stage after processing.
     *
     * @return the result of stage after processing.
     */
    public PipelineStageStatus getPipelineStageStatus() {
        return pipelineStageStatus;
    }

    /**
     * Returns any properties that need to be added to the pipeline.
     *
     * @return any properties that need to be added to the pipeline.
     */
    public ImmutableMap<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns any messages that need to be sent to the pipeline.
     *
     * @return any messages that need to be sent to the pipeline.
     */
    public ImmutableSet<String> getMessages() {
        return messages;
    }

    /**
     * Returns a set of additional pipeline stages that need to inserted into the pipeline pipeline after the current stage.
     *
     * @return a set of additional pipeline stages that need to inserted into the pipeline pipeline after the current stage.
     */
    public ImmutableSet<Class<? extends PipelineStage>> getAdditionalPipelineStages() {
        return additionalPipelineStages;
    }

    /**
     * Returns a Pipeline Stage Result Builder.
     *
     * @param pipelineId
     *         the id of the pipeline being processed by the stage
     * @param pipelineStageStatus
     *         the result of stage after processing
     * @return a Pipeline Stage Result Builder.
     */
    public static Builder builder(@NotNull String pipelineId, @NotNull PipelineStageStatus pipelineStageStatus) {
        return new PipelineStageResult.Builder(pipelineId, pipelineStageStatus);
    }

    /**
     * Pipeline Stage Result Builder
     */
    public static class Builder {

        private final String pipelineId;
        private final PipelineStageStatus pipelineStageStatus;
        private ImmutableMap<String, String> properties;
        private ImmutableSet<String> messages;
        private ImmutableSet<Class<? extends PipelineStage>> additionalPipelineStages;

        /**
         * Creates a new Pipeline Stage Result Builder.
         *
         * @param pipelineId
         *         the id of the pipeline being processed by the stage
         * @param pipelineStageStatus
         *         the result of stage after processing
         */
        public Builder(@NotNull String pipelineId, @NotNull PipelineStageStatus pipelineStageStatus) {
            this.pipelineId = pipelineId;
            this.pipelineStageStatus = pipelineStageStatus;
        }

        /**
         * Adds any properties that need to be added to the pipeline.
         *
         * @param properties
         *         any properties that need to be added to the pipeline
         * @return the Builder instance.
         */
        public Builder addProperties(ImmutableMap<String, String> properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Adds any messages that need to be sent to the pipeline.
         *
         * @param messages
         *         any messages that need to be sent to the pipeline
         * @return the Builder instance.
         */
        public Builder addMessages(ImmutableSet<String> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Adds a set of additional pipeline stages that need to inserted into the pipeline pipeline after the current stage.
         *
         * @param additionalPipelineStages
         *         a set of additional pipeline stages that need to inserted into the pipeline pipeline after the current stage.
         * @return the Builder instance.
         */
        public Builder addAdditionalPipelineStages(ImmutableSet<Class<? extends PipelineStage>> additionalPipelineStages) {
            this.additionalPipelineStages = additionalPipelineStages;
            return this;
        }

        public PipelineStageResult build() {
            return new PipelineStageResult(pipelineId, pipelineStageStatus, Optional.ofNullable(properties).orElse(ImmutableMap.of()), Optional.ofNullable(messages).orElse(ImmutableSet.of()),
                    Optional.ofNullable(additionalPipelineStages).orElse(ImmutableSet.of()));
        }
    }

}
