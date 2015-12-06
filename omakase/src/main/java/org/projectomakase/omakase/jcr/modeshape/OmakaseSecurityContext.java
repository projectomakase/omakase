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

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.security.OmakaseSecurity;
import org.jboss.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.ModeShapePermissions;
import org.modeshape.jcr.security.AuthorizationProvider;
import org.modeshape.jcr.security.JaasSecurityContext;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.basic.BasicPath;
import org.modeshape.jcr.value.basic.BasicPathSegment;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom ModeShape SecurityContext that implements the AuthorizationProvider in order to provide finer grain
 * authorization than the simple role based authorization provided by ModeShape.
 *
 * @author Richard Lucas
 */
public class OmakaseSecurityContext extends JaasSecurityContext implements AuthorizationProvider {

    private static final Logger LOGGER = Logger.getLogger(OmakaseSecurityContext.class);

    private Subject subject;

    public OmakaseSecurityContext(Subject subject) {
        super(subject);
        this.subject = subject;
    }

    @Override
    public boolean hasPermission(ExecutionContext context, String repositoryName, String repositorySourceName,
                                 String workspaceName, Path absPath, String... actions) {

        boolean hasPermission = false;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("JAAS - " + subject.toString());
            LOGGER.trace(
                "Validating node " + absPath.getString() + " in repository " + repositoryName + " and workspace "
                + workspaceName + " for the following actions " + Arrays.toString(actions));
        }

        if (doesNodeBelongsToOrganization(absPath) && OmakaseSecurity.isAuthorized(subject, getConsolidatedActions(actions))) {
            hasPermission = true;
        }

        return hasPermission;
    }

    private static ImmutableSet<String> getConsolidatedActions(String... actions) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String action : actions) {
            switch (action) {
            case ModeShapePermissions.READ:
                builder.add("read");
                break;
            case ModeShapePermissions.REGISTER_NAMESPACE:
            case ModeShapePermissions.REGISTER_TYPE:
            case ModeShapePermissions.UNLOCK_ANY:
            case ModeShapePermissions.CREATE_WORKSPACE:
            case ModeShapePermissions.DELETE_WORKSPACE:
            case ModeShapePermissions.MONITOR:
            case ModeShapePermissions.INDEX_WORKSPACE:
                builder.add("admin");
                break;
            default:
                builder.add("readwrite");
                break;
            }
        }
        return builder.build();
    }

    private static boolean doesNodeBelongsToOrganization(Path absPath) {
        boolean belongs = false;
        String organization = OmakaseSecurity.getCurrentOrganization();
        if ("default".equals(organization)) {
            belongs = true;
        } else {
            List<Path.Segment> segments = new ArrayList<>();
            segments.add(new BasicPathSegment(new BasicName("", "organizations")));
            segments.add(new BasicPathSegment(new BasicName("", organization)));
            Path orgPath = new BasicPath(segments, true);
            if (absPath.equals(orgPath) || absPath.isDescendantOf(orgPath)) {
                belongs = true;
            }
        }
        return belongs;
    }
}
