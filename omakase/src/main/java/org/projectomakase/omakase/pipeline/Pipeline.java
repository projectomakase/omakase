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

import com.google.common.collect.ImmutableSet;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;
import org.projectomakase.omakase.commons.collectors.ImmutableSetCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.pipeline.stage.PipelineFailureStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pipeline instances are used to define and track the progress of a pipeline.
 * <p>
 * A pipeline consists of an ordered list of stages that need to be actioned as part of the pipeline.
 * </p>
 * The pipeline is used to:
 * <ul>
 * <li>track the current pipeline stage</li>
 * <li>track the status of the current pipeline stage</li>
 * <li>track the overall status of the pipeline</li>
 * </ul>
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:pipeline"})
public class Pipeline extends JcrEntity {

    @JcrProperty(name = "omakase:objectId")
    private String objectId;
    @JcrProperty(name = "omakase:object")
    private String object;
    @JcrProperty(name = "omakase:status")
    private PipelineStatus status = PipelineStatus.QUEUED;
    @JcrProperty(name = "omakase:statusOfCurrentStage")
    private PipelineStageStatus statusOfCurrentStage = PipelineStageStatus.QUEUED;
    @JcrProperty(name = "omakase:currentStage")
    private int currentStage = 0;
    @JcrProperty(name = "omakase:stages")
    private List<String> stages;
    @JcrProperty(name = "omakase:failureStage")
    private String failureStage;
    @JcrProperty(name = "omakase:callbackListenerId")
    private String callbackListenerId = "";
    @JcrProperty
    private Map<String, String> properties = new HashMap<>();

    public Pipeline() {
        // required by JCROM
    }

    /**
     * Creates a new pipeline.
     *
     * @param objectId
     *         the id of the object the pipeline pipeline is processing e.g. a job
     * @param object
     *         the type of the object the pipeline pipeline is processing e.g. job
     * @param callbackListenerId
     *         the id of the callback listener that handles pipeline callback events
     * @param stages
     *         an ordered list of pipeline stages that make up the pipeline pipeline
     * @param failureStage
     *         the failure stage
     */
    public Pipeline(String objectId, String object, String callbackListenerId, List<Class<? extends PipelineStage>> stages, Class<? extends PipelineFailureStage> failureStage) {
        this.objectId = objectId;
        this.object = object;
        this.callbackListenerId = callbackListenerId;
        this.stages = stages.stream().map(Class::getName).collect(Collectors.toList());
        this.failureStage = Optional.ofNullable(failureStage).map(Class::getName).orElse(null);
    }

    /**
     * Returns the id of the object the pipeline pipeline is processing e.g. a job
     *
     * @return the id of the object the pipeline pipeline is processing e.g. a job
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Returns the type of the object the pipeline pipeline is processing e.g. job
     *
     * @return the type of the object the pipeline pipeline is processing e.g. job
     */
    public String getObject() {
        return object;
    }

    /**
     * Returns the overall status of the pipeline.
     *
     * @return the overall status of the pipeline.
     */
    public PipelineStatus getStatus() {
        return status;
    }

    /**
     * Sets the overall status of the pipeline.
     *
     * @param status
     *         the pipeline status.
     */
    public void setStatus(PipelineStatus status) {
        this.status = status;
    }

    /**
     * Increments the current pipeline stage index pointer to point to the next stage.
     */
    public void incrementCurrentStage() {
        this.currentStage++;
    }

    /**
     * Returns an ordered immutable set of the classes that make up the pipeline pipeline.
     *
     * @return an ordered immutable set of the classes that make up the pipeline pipeline.
     */
    @SuppressWarnings("unchecked")
    public ImmutableSet<Class<? extends PipelineStage>> getPipelineStages() {
        return stages.stream().map(stage -> Throwables.returnableInstance(() -> (Class<PipelineStage>) Class.forName(stage))).collect(ImmutableSetCollector.toImmutableSet());
    }

    /**
     * Returns the {@link java.lang.Class} of the current pipeline stage.
     *
     * @return the {@link java.lang.Class} of the current pipeline stage.
     */
    @SuppressWarnings("unchecked")
    public Class<PipelineStage> getCurrentPipelineStage() {
        return Throwables.returnableInstance(() -> (Class<PipelineStage>) Class.forName(stages.get(currentStage)));
    }

    /**
     * Adds a new pipeline stage to the pipeline after the current stage.
     *
     * @param newPipelineStage
     *         the new pipeline stage
     */
    public void addNewPipelineStageAfterCurrentStage(Class<? extends PipelineStage> newPipelineStage) {
        int index = currentStage + 1;
        stages.add(index, newPipelineStage.getName());
    }

    /**
     * Returns the {@link PipelineStageStatus} of the current pipeline stage.
     *
     * @return the {@link PipelineStageStatus} of the current pipeline stage.
     */
    public PipelineStageStatus getStatusOfCurrentStage() {
        return statusOfCurrentStage;
    }

    /**
     * Sets the status of the current stage.
     *
     * @param statusOfCurrentStage
     *         the status of the current stage
     */
    public void setStatusOfCurrentStage(PipelineStageStatus statusOfCurrentStage) {
        this.statusOfCurrentStage = statusOfCurrentStage;
    }

    @SuppressWarnings("unchecked")
    public Optional<Class<PipelineFailureStage>> getFailureStage() {
        return Optional.ofNullable(Throwables.returnableInstance(() -> (Class<PipelineFailureStage>) Class.forName(failureStage)));
    }

    /**
     * Returns the id of the callback listener that handles pipeline callback events
     *
     * @return the id of the callback listener that handles pipeline callback events
     */
    public String getCallbackListenerId() {
        return callbackListenerId;
    }

    /**
     * Returns a map of name/value pairs stored on the pipeline.
     * <p>
     * This is used to preserve and pass properties between the different stages in the pipeline pipeline.
     * </p>
     *
     * @return a map of name/value pairs stored on the pipeline.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns true if the current pipeline stage is the first stage in the pipeline.
     *
     * @return true if the current pipeline stage is the first stage in the pipeline.
     */
    public boolean isFirstPipelineStage() {
        return currentStage == 0;
    }

    /**
     * Returns true if the current pipeline stage is the last stage in the pipeline.
     *
     * @return true if the current pipeline stage is the last stage in the pipeline.
     */
    public boolean isLastPipelineStage() {
        return currentStage == (stages.size() - 1);
    }

    @Override
    public String toString() {
        return "Pipeline{" +
                "objectId='" + objectId + '\'' +
                ", object='" + object + '\'' +
                ", status=" + status +
                ", statusOfCurrentStage=" + statusOfCurrentStage +
                ", currentStage=" + currentStage +
                ", stages=" + stages +
                ", callbackListenerId='" + callbackListenerId + '\'' +
                '}';
    }
}
