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

/**
 * An immutable context that encapsulates the current state of the pipeline.
 * <p>
 * The context is used by the pipeline framework to expose the pipeline information to the {@link PipelineStage} implementations. The implementations are not
 * allowed to directly modify the pipeline which is why an immutable context is passed instead of the pipeline object.
 * </p>
 *
 * @author Richard Lucas
 */
public class PipelineContext {

    private final String pipelineId;
    private final String objectId;
    private final String object;
    private final String callbackListenerId;
    private final ImmutableMap<String, String> properties;

    /**
     * Creates a new pipelineContext
     *
     * @param pipelineId
     *         the pipeline id
     * @param objectId
     *         the pipeline's object id
     * @param object
     *         the pipelines object name
     * @param callbackListenerId
     *         the callback listener id, that the pipeline observers events for.
     * @param properties
     *         pipeline properties
     */
    public PipelineContext(String pipelineId, String objectId, String object, String callbackListenerId, ImmutableMap<String, String> properties) {
        this.pipelineId = pipelineId;
        this.objectId = objectId;
        this.object = object;
        this.callbackListenerId = callbackListenerId;
        this.properties = properties;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObject() {
        return object;
    }

    public String getCallbackListenerId() {
        return callbackListenerId;
    }

    public ImmutableMap<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "PipelineContext{" +
                "pipelineId='" + pipelineId + '\'' +
                ", objectId='" + objectId + '\'' +
                ", object='" + object + '\'' +
                ", callbackListenerId='" + callbackListenerId + '\'' +
                '}';
    }
}
