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
package org.projectomakase.omakase.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.jcr.Template;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryConfigurationException;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SortOrder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

@RunWith(Arquillian.class)
public class RepositoryManagerIT {

    @Inject
    RepositoryManager repositoryManager;
    @Inject
    RepositoryFileDAO repositoryFileDAO;
    @Inject
    IdGenerator idGenerator;
    @Inject
    IntegrationTests integrationTests;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @After
    public void after() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @Test
    public void shouldCreateRepository() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository createdRepository = createFileSystemRepository();
            assertThat(createdRepository.getRepositoryName()).isEqualTo(createdRepository.getRepositoryName());
            assertThat(createdRepository).isEqualToComparingOnlyGivenFields(createdRepository, "description", "type");
            assertThat(createdRepository.getRepositoryConfiguration()).isNull();
            assertGeneratedFieldsAreCorrect(createdRepository, "admin");
        });
    }

    @Test
    public void shouldCreateRepositoryWithConfiguration() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository createdRepository = createFileSystemRepository();
            createdRepository.setRepositoryConfiguration(new FileRepositoryConfiguration("/test"));
            assertThat(createdRepository.getRepositoryName()).isEqualTo(createdRepository.getRepositoryName());
            assertThat(createdRepository).isEqualToComparingOnlyGivenFields(createdRepository, "description", "type");
            assertThat(createdRepository.getRepositoryConfiguration()).isNotNull().isInstanceOf(FileRepositoryConfiguration.class);
            assertThat(((FileRepositoryConfiguration) createdRepository.getRepositoryConfiguration()).getRoot()).isEqualTo("/test");
            assertGeneratedFieldsAreCorrect(createdRepository, "admin");
        });
    }

    @Test
    public void shouldFailToCreateRepositoryWithInvalidRepositoryType() {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                repositoryManager.createRepository(new Repository("test", "test description", "BAD"));
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(RepositoryConfigurationException.class);
                assertThat(e.getCause()).hasMessage("Invalid repository type BAD");
            }
        });
    }

    @Test
    public void shouldFindRepositoriesOrderedByDefault() {
        // default is created DESC
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                createRepository("test" + i, "FILE");
            }
            assertThat(repositoryManager.findRepositories(new RepositorySearchBuilder().build()).getRecords()).extracting("repositoryName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindRepositoriesOrderedByNameAscending() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                createRepository("test" + i, "FILE");
            }
            assertThat(repositoryManager.findRepositories(new RepositorySearchBuilder().orderBy(Repository.REPOSITORY_NAME).sortOrder(SortOrder.ASC).build()).getRecords()).extracting("repositoryName")
                    .containsExactly("test0", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void shouldFindRepositoriesOrderedByNameDescending() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                createRepository("test" + i, "FILE");
            }
            assertThat(repositoryManager.findRepositories(new RepositorySearchBuilder().orderBy(Repository.REPOSITORY_NAME).sortOrder(SortOrder.DESC).build()).getRecords()).extracting("repositoryName")
                    .containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindRepositoriesWhereNameEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            for (int i = 0; i < 5; i++) {
                createRepository("test" + i, "FILE");
            }
            assertThat(repositoryManager.findRepositories(new RepositorySearchBuilder().conditions(ImmutableList.of(new SearchCondition(Repository.REPOSITORY_NAME, Operator.EQ, "test3"))).build()).getRecords())
                    .extracting("repositoryName").containsExactly("test3");
        });
    }

    @Test
    public void shouldFindRepositoriesWhereTypeEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            createRepository("test0", "FILE");
            createRepository("test1", "S3");
            assertThat(repositoryManager.findRepositories(new RepositorySearchBuilder().conditions(ImmutableList.of(new SearchCondition(Repository.TYPE, Operator.EQ, "FILE"))).build()).getRecords())
                    .extracting("repositoryName").containsExactly("test0");
        });
    }

    @Test
    public void shouldGetRepositoriesForVariant() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            RepositoryFile repositoryFile = createRepositoryFile(repository);
            assertThat(repositoryManager.getRepositoriesForVariant(repositoryFile.getVariantId())).hasSize(1).extracting("repositoryName").containsExactly("test");
        });
    }

    @Test
    public void shouldReturnEmptyOptionalForIdThatDoesNotExist() {
        TestRunner.runAsUser("admin", "password", () -> assertThat(repositoryManager.getRepository("test").isPresent()).isFalse());
    }

    @Test
    public void shouldReturnEmptyOptionalForBlankRepositoryName() {
        TestRunner.runAsUser("admin", "password", () -> assertThat(repositoryManager.getRepository("").isPresent()).isFalse());
    }

    @Test
    public void shouldGetRepository() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository testRepository = createFileSystemRepository();
            Optional<Repository> repository = repositoryManager.getRepository(testRepository.getId());
            assertThat(repository.isPresent()).isTrue();
            assertThat(repository.get()).isEqualToComparingOnlyGivenFields(testRepository, "description", "type");
        });
    }

    @Test
    public void shouldGetRepositoryConfigurationType() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository testRepository = createFileSystemRepository();
            assertThat(repositoryManager.getRepositoryConfigurationType(testRepository.getId())).isEqualTo(FileRepositoryConfiguration.class);
        });
    }

    @Test
    public void shouldUpdateRepositoryConfiguration() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createFileSystemRepository();
            assertThat(repository.getRepositoryConfiguration()).isNull();

            // add new configuration
            repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test"));
            repository = repositoryManager.getRepository(repository.getId()).get();
            assertThat(repository.getRepositoryConfiguration()).isNotNull().isInstanceOf(FileRepositoryConfiguration.class);
            assertThat(((FileRepositoryConfiguration) repository.getRepositoryConfiguration()).getRoot()).isEqualTo("/test");

            // replace existing configuration
            repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test2"));
            repository = repositoryManager.getRepository(repository.getId()).get();
            assertThat(repository.getRepositoryConfiguration()).isNotNull().isInstanceOf(FileRepositoryConfiguration.class);
            assertThat(((FileRepositoryConfiguration) repository.getRepositoryConfiguration()).getRoot()).isEqualTo("/test2");

            // update existing configuration
            repository = repositoryManager.getRepository(repository.getId()).get();
            ((FileRepositoryConfiguration) repository.getRepositoryConfiguration()).setRoot("/test3");
            repositoryManager.updateRepositoryConfiguration(repository.getId(), repository.getRepositoryConfiguration());
            assertThat(repository.getRepositoryConfiguration()).isNotNull().isInstanceOf(FileRepositoryConfiguration.class);
            assertThat(((FileRepositoryConfiguration) repository.getRepositoryConfiguration()).getRoot()).isEqualTo("/test3");
        });
    }

    @Test
    public void shouldDeleteRepository() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createFileSystemRepository();
            assertThat(repository).isNotNull();
            repositoryManager.deleteRepository(repository.getId());
            assertThat(repositoryManager.getRepository(repository.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldFailToDeleteRepositoriesWithFiles() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            createRepositoryFile(repository);
            try {
                repositoryManager.deleteRepository(repository.getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("Unable to delete repository that contains files");
            }
        });
    }

    @Test
    public void shouldGetRepositoryUri() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            assertThat(repositoryManager.getRepositoryUri(repository.getId())).isEqualTo(Throwables.returnableInstance(() -> new URI("file:/test")));
        });
    }

    @Test
    public void shouldGetRepositoryTemplates() {
        TestRunner.runAsUser("admin", "password", () -> {
            ImmutableSet<Template> templates = repositoryManager.getRepositoryTemplates();
            assertThat(templates).extracting("type").contains("FILE", "S3");
        });
    }

    @Test
    public void shouldCreateRepositoryFile() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            RepositoryFile repositoryFile = createRepositoryFile(repository);
            assertThat(repositoryFile.getId()).isNotNull();
            assertThat(repositoryFile.getRelativePath()).isNotNull();
            assertThat(repositoryFile.getVariantId()).isNotNull();
        });
    }

    @Test
    public void shouldFailToCreateRepositoryFileNotFound() {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                repositoryManager.createRepositoryFile("a", "test");
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotFoundException.class);
                assertThat(e.getCause()).hasMessage("Repository test does not exist");
            }
        });
    }

    @Test
    public void shouldFailToCreateRepositoryFileRepositoryNotConfigured() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createFileSystemRepository();
            try {
                createRepositoryFile(repository);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(RepositoryConfigurationException.class);
                assertThat(e.getCause()).hasMessage("Repository " + repository.getId() + " is not configured");
            }
        });
    }

    @Test
    public void shouldGetRepositoryFile() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            RepositoryFile repositoryFile = createRepositoryFile(repository);
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).get()).isEqualToComparingFieldByField(repositoryFile);
        });
    }

    @Test
    public void shouldGetRepositoryFilesForVariant() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            String variantId = idGenerator.getId();
            RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variantId, repository.getId());
            assertThat(repositoryManager.getRepositoryFilesForVariant(repository.getId(), variantId)).hasSize(1).usingFieldByFieldElementComparator().contains(repositoryFile);
        });
    }

    @Test
    public void shouldDeleteRepositoryFile() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            RepositoryFile repositoryFile = createRepositoryFile(repository);
            assertThat(repositoryFile.getId()).isNotNull();
            repositoryManager.deleteRepositoryFile(repository.getId(), repositoryFile.getId());
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldFailToDeleteRepositoryFileDoesNotExist() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            String badId = new IdGenerator().getId();
            try {
                repositoryManager.deleteRepositoryFile(repository.getId(), badId);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseExactlyInstanceOf(NotFoundException.class);
                assertThat(e.getCause()).hasMessage("Repository file " + badId + " does not exist in repository " + repository.getId());
            }

        });
    }

    @Test
    public void shouldDeleteVariantFromRepository() {
        TestRunner.runAsUser("admin", "password", () -> {
            Repository repository = createConfiguredFileSystemRepository();
            RepositoryFile repositoryFile = createRepositoryFile(repository);
            assertThat(repositoryFileDAO.findByVariantIdAndRepositoryId(repositoryFile.getVariantId(), repository.getId())).isNotEmpty();
            repositoryManager.deleteVariantFromRepository(repository.getId(), repositoryFile.getVariantId());
            assertThat(repositoryFileDAO.findByVariantIdAndRepositoryId(repositoryFile.getVariantId(), repository.getId())).isEmpty();
        });
    }

    private Repository createRepository(String name, String type) {
        return repositoryManager.createRepository(new Repository(name, "test", type));
    }

    private Repository createFileSystemRepository() {
        return repositoryManager.createRepository(new Repository("test", "test", "FILE"));
    }

    private Repository createConfiguredFileSystemRepository() {
        Repository repository = repositoryManager.createRepository(new Repository("test", "test", "FILE"));
        repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test"));
        return repository;
    }

    private RepositoryFile createRepositoryFile(Repository repository) {
        return repositoryManager.createRepositoryFile(new IdGenerator().getId(), repository.getId());
    }

    private void assertGeneratedFieldsAreCorrect(Repository repository, String expectedUser) {
        assertThat(repository.getCreated()).isNotNull();
        assertThat(repository.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(repository.getLastModified()).isNotNull();
        assertThat(repository.getLastModifiedBy()).isEqualTo(expectedUser);
    }
}