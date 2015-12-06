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

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

/**
 * Provides useful security methods.
 *
 * @author Richard Lucas
 */
public final class OmakaseSecurity {

    private static final Logger LOGGER = Logger.getLogger(OmakaseSecurity.class);

    private OmakaseSecurity() {
        // hides implicit constructor
    }

    /**
     * Executes the privileged action as the system user
     *
     * @param action
     *         the action to run
     * @param <T>
     *         the actions return type
     * @return the value returned by the action's run method.
     * @throws OmakaseRuntimeException
     *         if the action throws an exception.
     */
    public static <T> T doAsSystem(PrivilegedExceptionAction<T> action) {
        return Throwables.returnableInstance(() -> doAs("system", "YBaL869w", action));
    }

    /**
     * Executes the privileged action as the specified user
     *
     * @param username
     *         the username
     * @param password
     *         the password
     * @param action
     *         the action to run
     * @param <T>
     *         the actions return type
     * @return the value returned by the action's run method.
     * @throws OmakaseRuntimeException
     *         if the action throws an exception.
     */
    public static <T> T doAs(String username, String password, PrivilegedExceptionAction<T> action) {
        return Throwables.returnableInstance(() -> {
            SecurityContext incomingSecurityContext = SecurityContextAssociation.getSecurityContext();
            LoginContext loginContext = OmakaseLoginContextFactory.createLoginContext(username, password);
            loginContext.login();
            try {
                Subject subject = loginContext.getSubject();
                SecurityContext doAsSecurityContext = SecurityContextFactory
                    .createSecurityContext(new SimplePrincipal(username), password, subject,
                                           OmakaseLoginContextFactory.SECURITY_DOMAIN);
                SecurityContextAssociation.setSecurityContext(doAsSecurityContext);
                return Subject.doAs(subject, action);
            } finally {
                loginContext.logout();
                SecurityContextAssociation.setSecurityContext(incomingSecurityContext);
            }
        });
    }

    /**
     * Returns the organization for the current user.
     *
     * @return the organization for the current user.
     */
    public static String getCurrentOrganization() {
        // in the future the organization will be retrieved from the users subject.
        return "default";
    }

    public static boolean isAuthorized(Subject subject, Set<String> actions) {
        boolean hasPermission = true;
        if (subject == null || subject.getPrincipals() == null) {
            hasPermission = false;
        } else {

            ImmutableSet<String> usersRoleNames = SubjectReader.getRoleNamesFromSubject(subject);

            for (String action : actions) {
                switch (action) {
                case "read":
                    hasPermission &= usersRoleNames.contains("readonly") || usersRoleNames.contains("readwrite") ||
                                     usersRoleNames.contains("admin");
                    break;
                case "admin":
                    hasPermission &= usersRoleNames.contains("admin");
                    break;
                default:
                    hasPermission &= usersRoleNames.contains("readwrite") || usersRoleNames.contains("admin");
                    break;
                }
            }
        }
        return hasPermission;
    }
}
