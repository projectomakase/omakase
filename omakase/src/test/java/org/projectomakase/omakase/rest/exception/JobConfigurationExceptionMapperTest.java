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
package org.projectomakase.omakase.rest.exception;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.job.configuration.JobConfigurationException;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusModel;
import org.projectomakase.omakase.rest.model.v1.ResponseStatusValue;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class JobConfigurationExceptionMapperTest {

    @Test
    public void shouldReturnBadRequestResponse() throws Exception {
        JobConfigurationException jobConfigurationException = new JobConfigurationException(ImmutableList.of("bad", "worse", "catastrophic"));
        JobConfigurationExceptionMapper mapper = new JobConfigurationExceptionMapper();
        Response response = mapper.toResponse(jobConfigurationException);
        assertThat(response.getStatus()).isEqualTo(400);
        Assertions.assertThat((ResponseStatusModel) response.getEntity()).isEqualToComparingFieldByField(
                new ResponseStatusModel(ResponseStatusValue.ERROR, "The job configuration contains validation errors: [bad, worse, catastrophic]", jobConfigurationException.toString()));

    }
}