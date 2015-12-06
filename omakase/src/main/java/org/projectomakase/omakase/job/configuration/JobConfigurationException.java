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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown if the job configuration is invalid.
 *
 * @author Richard Lucas
 */
public class JobConfigurationException extends RuntimeException {

    private final List<String> validationErrors;

    /**
     * Constructs a new {@link JobConfigurationException}.
     *
     * @param validationErrors
     *         a list of validation messages.
     */
    public JobConfigurationException(List<String> validationErrors) {
        super("The job configuration contains validation errors");
        this.validationErrors = validationErrors;
    }

    @Override
    public String getMessage() {
        String errors = getValidationErrors().stream().collect(Collectors.joining(", "));
        return String.format("%s: [%s]", super.getMessage(), errors);
    }

    /**
     * Returns the list of validation errors.
     *
     * @return the list of validation errors.
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
