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

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.callback.CallbackEvent;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;
import org.projectomakase.omakase.pipeline.stage.PipelineStage;
import org.projectomakase.omakase.pipeline.stage.PipelineStageResult;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class PipelineTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldAddNewPipelineStageToPipeline() {
        Pipeline pipeline = new Pipeline("abc", "test", "test", ImmutableList.of(StageOne.class, StageTwo.class, StageThree.class), null);
        assertThat(pipeline.getPipelineStages()).containsExactly(StageOne.class, StageTwo.class, StageThree.class);
        pipeline.addNewPipelineStageAfterCurrentStage(NewStage.class);
        assertThat(pipeline.getPipelineStages()).containsExactly(StageOne.class, NewStage.class, StageTwo.class, StageThree.class);
    }

    public class StageOne extends TestStage {
    }

    public class StageTwo extends TestStage {
    }

    public class StageThree extends TestStage {
    }

    public class NewStage extends TestStage {
    }

    class TestStage implements PipelineStage {
        @Override
        public PipelineStageResult prepare(PipelineContext pipelineContext) {
            return null;
        }

        @Override
        public PipelineStageResult onCallback(PipelineContext pipelineContext, CallbackEvent callbackEvent) {
            return null;
        }
    }

}