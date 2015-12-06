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
package org.projectomakase.omakase.jcr.modeshape;

import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.security.JaasProvider;

import javax.jcr.Credentials;
import javax.security.auth.login.LoginException;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import java.util.Map;

/**
 * Custom ModeShape AuthenticationProvider. Delegates authentication to the standard modeshape JaasProvider and sets a custom SecurityContext on the ExecutionContext once authentication has
 * completed.
 *
 * @author Richard Lucas
 */
public class OmakaseAuthenticationProvider extends JaasProvider {

    private static final Logger LOGGER = Logger.getLogger(OmakaseAuthenticationProvider.class);

    private static final String DEFAULT_POLICY = "omakase-security";

    private String name = "OmakaseAuthenticationProvider";

    /**
     * Create a provider for authentication and authorization, using the default name for the login configuration.
     *
     * @throws javax.security.auth.login.LoginException
     *         if the caller-specified <code>name</code> does not appear in the <code>Configuration</code> and
     *         there is no <code>Configuration</code> entry for "<i>other</i>", or if the
     *         <i>auth.login.defaultCallbackHandler</i> security property was set, but the implementation class could not be
     *         loaded.
     *
     */
    public OmakaseAuthenticationProvider() throws LoginException {
        super(DEFAULT_POLICY);
    }

    /**
     * Create a provider for authentication and authorization, using the supplied name for the login configuration.
     *
     * @param policyName
     *         the name that will be used for the login context
     * @throws javax.security.auth.login.LoginException
     *         if the caller-specified <code>name</code> does not appear in the <code>Configuration</code> and
     *         there is no <code>Configuration</code> entry for "<i>other</i>", or if the
     *         <i>auth.login.defaultCallbackHandler</i> security property was set, but the implementation class could not be
     *         loaded.
     *
     */
    public OmakaseAuthenticationProvider(final String policyName) throws LoginException {
        super(policyName);
    }

    @Override
    public ExecutionContext authenticate(final Credentials credentials, final String repositoryName, final String workspaceName, final ExecutionContext repositoryContext,
                                         final Map<String, Object> sessionAttributes) {
        // We discard the OmakaseCredentials and pass null to JaasProvider in order to force it to look up the credentials from the subject.
        return super.authenticate(null, repositoryName, workspaceName, repositoryContext, sessionAttributes).with(new OmakaseSecurityContext(getSubject()));
    }

    public String getName() {
        return name;
    }

    private javax.security.auth.Subject getSubject() {
        javax.security.auth.Subject subject = null;
        try {
            getClass().getClassLoader().loadClass("javax.security.jacc.PolicyContext");
            subject = (javax.security.auth.Subject) PolicyContext.getContext(SecurityConstants.SUBJECT_CONTEXT_KEY);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to find 'javax.security.jacc.PolicyContext'", e);
        } catch (PolicyContextException e) {
            LOGGER.error("Failed to get subject from 'javax.security.jacc.PolicyContext'", e);
        }
        return subject;
    }

}
