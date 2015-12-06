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
import org.jboss.security.SimpleGroup;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Provides methods for reading the contents of a JAAS Subject
 *
 * @author Richard Lucas
 */
public final class SubjectReader {

    private SubjectReader() {
        // hide default constructor
    }

    /**
     * Returns the caller principle from the subject
     *
     * @param subject
     *         the subject
     * @return the caller principle from the subject
     */
    public static Optional<String> getCallerPrincipleFromSubject(final Subject subject) {
        Optional<String> caller = Optional.empty();
        Stream<Principal> stream = subject.getPrincipals().stream();
        Optional<Principal> principal = stream.filter(principle -> principle instanceof SimpleGroup).filter(group -> "CallerPrincipal".equals(group.getName())).findFirst();
        if (principal.isPresent()) {
            SimpleGroup simpleGroup = (SimpleGroup) principal.get();
            if (simpleGroup.members().hasMoreElements()) {
                caller = Optional.ofNullable(simpleGroup.members().nextElement().getName());
            }
        }
        return caller;
    }

    /**
     * Returns a set of role names from the subject
     *
     * @param subject
     *         the subject
     * @return a set of role names from the subject
     */
    public static ImmutableSet<String> getRoleNamesFromSubject(final Subject subject) {
        if (subject == null || subject.getPrincipals() == null || subject.getPrincipals().isEmpty()) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<String> roles = ImmutableSet.builder();
        Collections.list(((SimpleGroup) subject.getPrincipals().stream().filter(principle -> principle instanceof SimpleGroup).filter(group -> "Roles".equals(group.getName())).findFirst().get())
                .members()).stream().forEach(role -> roles.add(role.getName()));
        return roles.build();
    }
}
