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

import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.projectomakase.omakase.rest.response.Responses;
import org.jboss.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * JAX-RS Exception mapper for {@link RepositoryConfigurationException}.
 *
 * @author Richard Lucas
 */
@Provider
public class RepositoryConfigurationExceptionMapper implements ExceptionMapper<RepositoryConfigurationException> {

    private static final Logger LOGGER = Logger.getLogger(RepositoryConfigurationExceptionMapper.class);

    @Override
    public Response toResponse(RepositoryConfigurationException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Responses.errorResponse(Response.status(Response.Status.BAD_REQUEST).build(), exception.getMessage(), exception, LOGGER.isInfoEnabled());
    }
}
