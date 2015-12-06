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
package org.projectomakase.omakase.assertions;

import org.projectomakase.omakase.pipeline.Pipeline;
import org.projectomakase.omakase.pipeline.stage.PipelineFailureStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/**
 * @author Richard Lucas
 */
public class PipelineAssert extends AbstractAssert<PipelineAssert, Pipeline> {

    public PipelineAssert(Pipeline actual) {
        super(actual, PipelineAssert.class);
    }

    public final PipelineAssert hasObjectId(String objectId) {
        isNotNull();
        Assertions.assertThat(actual.getObjectId()).isEqualTo(objectId);
        return this;
    }

    public final PipelineAssert hasObject(String object) {
        isNotNull();
        Assertions.assertThat(actual.getObject()).contains(object);
        return this;
    }

    public final PipelineAssert hasCallbackListenerId(String callbackListenerId) {
        isNotNull();
        Assertions.assertThat(actual.getCallbackListenerId()).contains(callbackListenerId);
        return this;
    }

    @SafeVarargs
    public final PipelineAssert hasPipelineStages(Class<? extends PipelineStage>... stages) {
        isNotNull();
        Assertions.assertThat(actual.getPipelineStages()).containsExactly(stages);
        return this;
    }

    public final PipelineAssert hasPipelineFailureStage(Class<? extends PipelineFailureStage> failureStage) {
        isNotNull();
        Assertions.assertThat(actual.getFailureStage().get()).isEqualTo(failureStage);
        return this;
    }
}
