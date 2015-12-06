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

import org.projectomakase.omakase.rest.response.Responses;
import org.jboss.logging.Logger;

import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {

    private static final Logger LOGGER = Logger.getLogger(EJBExceptionMapper.class);

    @Context
    Providers providers;

    @SuppressWarnings("unchecked")
    @Override
    public Response toResponse(EJBException exception) {
        if (exception.getCausedByException() == null) {
            LOGGER.error(exception.getMessage(), exception);
            return Responses.errorResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(), "Internal Server Error", exception, LOGGER.isInfoEnabled());
        } else {
            Class cause = exception.getCausedByException().getClass();
            ExceptionMapper mapper = providers.getExceptionMapper(cause);
            if (mapper == null) {
                LOGGER.error(exception.getMessage(), exception);
                return Responses.errorResponse(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(), "Internal Server Error", exception, LOGGER.isInfoEnabled());
            } else {
                return mapper.toResponse(exception.getCausedByException());
            }
        }
    }
}
