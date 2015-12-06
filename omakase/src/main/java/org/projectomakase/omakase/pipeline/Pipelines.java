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

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;

import java.util.Optional;

/**
 * @author Richard Lucas
 */
public final class Pipelines {

    private Pipelines() {
        // hide default constructor
    }

    /**
     * Returns the property from the given {@link PipelineContext} as a String.
     *
     * @param pipelineContext
     *         the pipeline context
     * @param property
     *         the property name
     * @return the property the property from the given {@link PipelineContext} as a String.
     * @throws OmakaseRuntimeException
     *         if the property does not exist.
     */
    public static String getPipelineProperty(PipelineContext pipelineContext, String property) {
        return Optional.ofNullable(pipelineContext.getProperties().get(property))
                .orElseThrow(() -> new OmakaseRuntimeException("Unable to find " + property + " property on pipeline " + pipelineContext.getPipelineId()));
    }
}
