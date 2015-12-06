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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Provides methods for validating {@link JobConfiguration} implementations.
 *
 * @author Richard Lucas
 */
public class JobConfigurationValidator {

    @Inject
    IngestJobConfigurationValidator ingestJobConfigurationValidator;
    @Inject
    ExportJobConfigurationValidator exportJobConfigurationValidator;
    @Inject
    ReplicationJobConfigurationValidator replicationJobConfigurationValidator;

    /**
     * Validates the specified {@link IngestJobConfiguration}.
     *
     * @param jobConfiguration
     *         the specified {@link IngestJobConfiguration}
     * @throws JobConfigurationException
     *         if the configuration is invalid
     */
    public void validate(@NotNull IngestJobConfiguration jobConfiguration) {
        handleValidationErrors(ingestJobConfigurationValidator.validate(jobConfiguration));
    }

    /**
     * Validates the specified {@link ExportJobConfiguration}.
     *
     * @param jobConfiguration
     *         the specified {@link ExportJobConfiguration}
     * @throws JobConfigurationException
     *         if the configuration is invalid
     */
    public void validate(@NotNull ExportJobConfiguration jobConfiguration) {
        handleValidationErrors(exportJobConfigurationValidator.validate(jobConfiguration));
    }

    /**
     * Validates the specified {@link ReplicationJobConfiguration}.
     *
     * @param jobConfiguration
     *         the specified {@link ReplicationJobConfiguration}
     * @throws JobConfigurationException
     *         if the configuration is invalid
     */
    public void validate(@NotNull ReplicationJobConfiguration jobConfiguration) {
        handleValidationErrors(replicationJobConfigurationValidator.validate(jobConfiguration));
    }


    private static void handleValidationErrors(List<String> validationErrors) {
        if (!validationErrors.isEmpty()) {
            throw new JobConfigurationException(validationErrors);
        }
    }
}
