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

import com.google.common.base.Strings;
import org.projectomakase.omakase.security.OmakaseSecurity;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * CDI producer used when injecting the organization node path for the current user.
 *
 * @author Richard Lucas
 */
public class OrganizationNodePathProducer {

    @Produces
    @OrganizationNodePath
    public String getCurrentOrgNodePath(InjectionPoint injectionPoint) {
        String subPath = injectionPoint.getAnnotated().getAnnotation(OrganizationNodePath.class).value();
        String path = "/organizations/" + OmakaseSecurity.getCurrentOrganization();
        if (!Strings.isNullOrEmpty(subPath)) {
            path += "/" + removePathSeparatorFromStart(subPath);
        }
        return path;
    }

    private static  String removePathSeparatorFromStart(String subPath) {
        if (subPath.startsWith("/")) {
            return subPath.replaceFirst("/", "");
        } else {
            return subPath;
        }
    }
}
