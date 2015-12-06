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
package org.projectomakase.omakase.job.message;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class WorkerMessageSQL2QueryBuilderTest {

    @Test
    public void shouldBuildQuery() throws Exception {
        WorkerMessageSQL2QueryBuilder workerMessageSQL2QueryBuilder = new WorkerMessageSQL2QueryBuilder("1");
        assertThat(workerMessageSQL2QueryBuilder.build()).isEqualTo(
                "SELECT node.* FROM [omakase:message] AS node JOIN [omakase:task] as task ON ISDESCENDANTNODE('node', 'task') " +
                        "JOIN [omakase:worker] as worker ON task.[jcr:name] = worker.[omakase:tasks] WHERE worker.[jcr:name] = '1'");
    }
}