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
import org.jboss.security.SimplePrincipal;
import org.junit.Test;

import javax.security.auth.Subject;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class SubjectReaderTest {

    @Test
    public void shouldGetCallerPrincipleFromSubject() throws Exception {
        assertThat(SubjectReader.getCallerPrincipleFromSubject(createSubjectWithCallerPrinciple("test"))).isPresent().contains("test");
    }

    @Test
    public void shouldGetRoleNamesFromSubject() throws Exception {
        assertThat(SubjectReader.getRoleNamesFromSubject(createSubjectWithRoles(ImmutableSet.of("test")))).containsExactly("test");
    }

    private static Subject createSubjectWithRoles(Set<String> principals) {
        final Subject subject = new Subject();
        SimpleGroup simpleGroup = new SimpleGroup("Roles");
        principals.stream().map(SimplePrincipal::new).forEach(simpleGroup::addMember);
        subject.getPrincipals().add(simpleGroup);
        return subject;
    }

    private static Subject createSubjectWithCallerPrinciple(String principal) {
        final Subject subject = new Subject();
        SimpleGroup simpleGroup = new SimpleGroup("CallerPrincipal");
        simpleGroup.addMember(new SimplePrincipal(principal));
        subject.getPrincipals().add(simpleGroup);
        return subject;
    }

}