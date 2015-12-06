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
package org.projectomakase.omakase.security;

import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.JBossCallbackHandler;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Factory that creates a new {@link javax.security.auth.login.LoginContext} for the 'omakase-security' domain.
 *
 * @author Richard Lucas
 */
public final class OmakaseLoginContextFactory {

    public static final String SECURITY_DOMAIN = "omakase-security";

    private OmakaseLoginContextFactory() {
        // Utility classes should not have a public constructor
    }

    /**
     * Creates a new {@link javax.security.auth.login.LoginContext} configured with the 'omakase-security' domain and simple username/password {@link javax.security.auth.callback.CallbackHandler}
     *
     * @param username
     *         the username that will be passed to the Login Module when authenticating.
     * @param password
     *         the password that will be passed to the Login Module when authenticating.
     * @return a new LoginContext.
     * @throws LoginException
     *         if an error occurs creating the LoginContext.
     */
    public static LoginContext createLoginContext(final String username, final String password) throws LoginException {
        return new LoginContext(SECURITY_DOMAIN, new Subject(), new JBossCallbackHandler(new SimplePrincipal(username), password));
    }
}
