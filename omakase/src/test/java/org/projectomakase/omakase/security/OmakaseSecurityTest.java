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
public class OmakaseSecurityTest {

    private static final Subject ADMIN = createSubject(ImmutableSet.of("readonly", "readwrite", "admin"));
    private static final Subject EDITOR = createSubject(ImmutableSet.of("readonly", "readwrite"));
    private static final Subject READER = createSubject(ImmutableSet.of("readonly"));

    @Test
    public void shouldGetCurrentOrganization() throws Exception {
        assertThat(OmakaseSecurity.getCurrentOrganization()).isEqualTo("default");
    }

    @Test
    public void shouldAFailToAuthorizeNullPrinciples() {
        Subject subject = new Subject();
        assertThat(OmakaseSecurity.isAuthorized(subject, ImmutableSet.of("read"))).isFalse();
    }

    @Test
    public void shouldAFailToAuthorizeEmptyPrinciples() {
        Subject subject = createSubject(ImmutableSet.of());
        assertThat(OmakaseSecurity.isAuthorized(subject, ImmutableSet.of("read"))).isFalse();
    }

    @Test
    public void shouldAuthorizeAdminToAdminister() {
        assertThat(OmakaseSecurity.isAuthorized(ADMIN, ImmutableSet.of("admin"))).isTrue();
    }

    @Test
    public void shouldAuthorizeAdminToEdit() {
        Subject subject = createSubject(ImmutableSet.of("read", "readwrite", "admin"));
        assertThat(OmakaseSecurity.isAuthorized(ADMIN, ImmutableSet.of("readwrite"))).isTrue();
    }

    @Test
    public void shouldAuthorizeAdminToRead() {
        Subject subject = createSubject(ImmutableSet.of("read", "readwrite", "admin"));
        assertThat(OmakaseSecurity.isAuthorized(ADMIN, ImmutableSet.of("read"))).isTrue();
    }

    @Test
    public void shouldAuthorizeEditorToEdit() {
        assertThat(OmakaseSecurity.isAuthorized(EDITOR, ImmutableSet.of("readwrite"))).isTrue();
    }

    @Test
    public void shouldAuthorizeEditorToRead() {
        assertThat(OmakaseSecurity.isAuthorized(EDITOR, ImmutableSet.of("read"))).isTrue();
    }

    @Test
    public void shouldFailAuthorizeEditorToAdminister() {
        assertThat(OmakaseSecurity.isAuthorized(EDITOR, ImmutableSet.of("admin"))).isFalse();
    }

    @Test
    public void shouldAuthorizeReaderToRead() {
        assertThat(OmakaseSecurity.isAuthorized(READER, ImmutableSet.of("read"))).isTrue();
    }

    @Test
    public void shouldFailAuthorizeReaderToAdminister() {
        assertThat(OmakaseSecurity.isAuthorized(READER, ImmutableSet.of("admin"))).isFalse();
    }

    @Test
    public void shouldFailAuthorizeReaderToEdit() {
        assertThat(OmakaseSecurity.isAuthorized(READER, ImmutableSet.of("edit"))).isFalse();
    }

    private static Subject createSubject(Set<String> principals) {
        final Subject subject = new Subject();
        SimpleGroup simpleGroup = new SimpleGroup("Roles");
        principals.stream().map(SimplePrincipal::new).forEach(simpleGroup::addMember);
        subject.getPrincipals().add(simpleGroup);
        return subject;
    }

}