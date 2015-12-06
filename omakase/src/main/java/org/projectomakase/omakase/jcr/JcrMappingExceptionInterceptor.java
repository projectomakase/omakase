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
package org.projectomakase.omakase.jcr;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.jcr.RepositoryException;

/**
 * CDI Interceptor that intercepts JcrMappingExceptions thrown by Jcrom, extracts the underling JCR RepositoryException and wraps in it the relevant Omakase Exception.
 *
 * @author Richard Lucas
 */
@JcrMappingException
@Interceptor
public class JcrMappingExceptionInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } catch (org.jcrom.JcrMappingException e) {
            if (e.getCause() instanceof RepositoryException) {
                return JcrThrowables.wrapJcrExceptionsWithReturn(() -> {
                    throw (RepositoryException) e.getCause();
                });
            } else {
                throw e;
            }
        }
    }
}
