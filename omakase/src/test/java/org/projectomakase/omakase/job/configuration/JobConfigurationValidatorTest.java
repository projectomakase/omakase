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
package org.projectomakase.omakase.job.configuration;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class JobConfigurationValidatorTest {

    private JobConfigurationValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new JobConfigurationValidator();
    }

    @Test
    public void shouldValidateIngestJobConfigurationWithNoErrors() throws Exception {
        IngestJobConfigurationValidator ingestJobConfigurationValidator = mock(IngestJobConfigurationValidator.class);
        validator.ingestJobConfigurationValidator = ingestJobConfigurationValidator;
        doReturn(ImmutableList.of()).when(ingestJobConfigurationValidator).validate(any(IngestJobConfiguration.class));
        try {
            validator.validate(new IngestJobConfiguration());
        } catch (JobConfigurationException e) {
            fail("Unexpected job configuration exception", e);
        }
    }

    @Test
    public void shouldValidateIngestJobConfigurationWithErrors() throws Exception {
        IngestJobConfigurationValidator ingestJobConfigurationValidator = mock(IngestJobConfigurationValidator.class);
        validator.ingestJobConfigurationValidator = ingestJobConfigurationValidator;
        doReturn(ImmutableList.of("bad", "worse", "catastrophic")).when(ingestJobConfigurationValidator).validate(any(IngestJobConfiguration.class));
        Throwable thrown = catchThrowable(() -> validator.validate(new IngestJobConfiguration()));
        assertThat(thrown).isExactlyInstanceOf(JobConfigurationException.class).hasMessage("The job configuration contains validation errors: [bad, worse, catastrophic]");
        assertThat(((JobConfigurationException) thrown).getValidationErrors()).containsExactly("bad", "worse", "catastrophic");
    }

    @Test
    public void shouldValidateExportJobConfigurationWithNoErrors() throws Exception {
        ExportJobConfigurationValidator exportJobConfigurationValidator = mock(ExportJobConfigurationValidator.class);
        validator.exportJobConfigurationValidator = exportJobConfigurationValidator;
        doReturn(ImmutableList.of()).when(exportJobConfigurationValidator).validate(any(ExportJobConfiguration.class));
        try {
            validator.validate(new ExportJobConfiguration());
        } catch (JobConfigurationException e) {
            fail("Unexpected job configuration exception", e);
        }
    }

    @Test
    public void shouldValidateExportJobConfigurationWithErrors() throws Exception {
        ExportJobConfigurationValidator exportJobConfigurationValidator = mock(ExportJobConfigurationValidator.class);
        validator.exportJobConfigurationValidator = exportJobConfigurationValidator;
        doReturn(ImmutableList.of("bad", "worse", "catastrophic")).when(exportJobConfigurationValidator).validate(any(ExportJobConfiguration.class));
        Throwable thrown = catchThrowable(() -> validator.validate(new ExportJobConfiguration()));
        assertThat(thrown).isExactlyInstanceOf(JobConfigurationException.class).hasMessage("The job configuration contains validation errors: [bad, worse, catastrophic]");
        assertThat(((JobConfigurationException) thrown).getValidationErrors()).containsExactly("bad", "worse", "catastrophic");
    }

    @Test
    public void shouldValidateReplicationJobConfigurationWithNoErrors() throws Exception {
        ReplicationJobConfigurationValidator replicationJobConfigurationValidator = mock(ReplicationJobConfigurationValidator.class);
        validator.replicationJobConfigurationValidator = replicationJobConfigurationValidator;
        doReturn(ImmutableList.of()).when(replicationJobConfigurationValidator).validate(any(ReplicationJobConfiguration.class));
        try {
            validator.validate(new ReplicationJobConfiguration());
        } catch (JobConfigurationException e) {
            fail("Unexpected job configuration exception", e);
        }
    }

    @Test
    public void shouldValidateReplicationJobConfigurationWithErrors() throws Exception {
        ReplicationJobConfigurationValidator replicationJobConfigurationValidator = mock(ReplicationJobConfigurationValidator.class);
        validator.replicationJobConfigurationValidator = replicationJobConfigurationValidator;
        doReturn(ImmutableList.of("bad", "worse", "catastrophic")).when(replicationJobConfigurationValidator).validate(any(ReplicationJobConfiguration.class));
        Throwable thrown = catchThrowable(() -> validator.validate(new ReplicationJobConfiguration()));
        assertThat(thrown).isExactlyInstanceOf(JobConfigurationException.class).hasMessage("The job configuration contains validation errors: [bad, worse, catastrophic]");
        assertThat(((JobConfigurationException) thrown).getValidationErrors()).containsExactly("bad", "worse", "catastrophic");
    }
}