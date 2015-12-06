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
package org.projectomakase.omakase.content;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.exceptions.NotAuthorizedException;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobDAO;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.IngestJobConfiguration;
import org.projectomakase.omakase.job.configuration.IngestJobFile;
import org.projectomakase.omakase.repository.api.Repository;
import org.projectomakase.omakase.repository.api.RepositoryFile;
import org.projectomakase.omakase.repository.RepositoryManager;
import org.projectomakase.omakase.repository.provider.file.FileRepositoryConfiguration;
import org.projectomakase.omakase.repository.provider.s3.S3RepositoryConfiguration;
import org.projectomakase.omakase.search.DefaultSearchBuilder;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchResult;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

@RunWith(Arquillian.class)
public class ContentManagerIT {

    private static final Logger LOGGER = Logger.getLogger(ContentManagerIT.class);

    @Inject
    ContentManager contentManager;
    @Inject
    JobManager jobManager;
    @Inject
    JobDAO jobDAO;
    @Inject
    RepositoryManager repositoryManager;
    @Inject
    IdGenerator idGenerator;
    @Inject
    IntegrationTests integrationTests;
    @Inject
    VariantRepositoryDAO variantRepositoryDAO;
    @Inject
    VariantFileDAO variantFileDAO;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }


    @Before
    public void before() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
        integrationTests.destroyJCRSession();
    }

    @After
    public void after() {
        integrationTests.destroyJCRSession();
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @Test
    public void shouldCreateAssetWithRequiredFields() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset());
            assertThat(asset.getAssetName()).isNull();
            assertThat(asset.getExternalIds()).isEmpty();
            assertThatAssetHasCreatedAndLastModified(asset, "admin");
            System.out.println(asset.getNodePath());
        });
    }

    @Test
    public void shouldCreateAssetWithAllFields() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            assertThat(asset.getAssetName()).isEqualTo("test");
            assertThat(asset.getExternalIds()).containsExactly("id1", "id2");
            assertThatAssetHasCreatedAndLastModified(asset, "admin");
        });
    }

    @Test
    public void shouldFailToCreateAssetNotAuthorized() throws Exception {
        TestRunner.runAsUser("reader", "password", () -> {
            try {
                contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotAuthorizedException.class);
            }
        });
    }

    @Test
    public void shouldGetAssets() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            contentManager.createAsset(new Asset("test1", ImmutableList.of("id1", "id2")));
            contentManager.createAsset(new Asset("test2", ImmutableList.of("id1", "id2")));
            contentManager.createAsset(new Asset("test3", ImmutableList.of("id1", "id2")));
            SearchResult<Asset> searchResult = contentManager.findAssets(new DefaultSearchBuilder().build());
            assertThat(searchResult.getTotalRecords()).isEqualTo(3);
            assertThat(searchResult.getRecords()).hasSize(3).extracting("assetName").containsOnly("test1", "test2", "test3");
        });
    }

    @Test
    public void shouldGetAssetById() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            assertThat(contentManager.getAsset(asset.getId()).get()).isEqualToComparingFieldByField(asset);
        });
    }

    @Test
    public void shouldReturnEmptyOptionalForAssetIdThatDoesNotExist() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> assertThat(contentManager.getAsset(idGenerator.getId()).isPresent()).isFalse());
    }

    @Test
    public void shouldUpdateAssetName() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", new ArrayList<>()));
            assertThat(asset.getAssetName()).isEqualTo("test");
            assertThat(asset.getExternalIds()).isEmpty();
            assertThatAssetHasCreatedAndLastModified(asset, "admin");


            asset.setAssetName("test1");

            Asset updatedAsset = contentManager.updateAsset(asset);
            assertThat(updatedAsset.getAssetName()).isEqualTo("test1");
            assertThat(updatedAsset.getExternalIds()).isEmpty();
            assertThatAssetHasCreatedAndLastModified(updatedAsset, "admin");
            assertThatDateAIsAfterDateB(updatedAsset.getLastModified(), asset.getLastModified());
        });
    }

    @Test
    public void shouldAddExternalIdsToAsset() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", new ArrayList<>()));
            assertThat(asset.getAssetName()).isEqualTo("test");
            assertThat(asset.getExternalIds()).isEmpty();
            assertThatAssetHasCreatedAndLastModified(asset, "admin");

            asset.setExternalIds(ImmutableList.of("id1", "id2"));

            Asset updatedAsset = contentManager.updateAsset(asset);
            assertThat(updatedAsset.getAssetName()).isEqualTo("test");
            assertThat(updatedAsset.getExternalIds()).containsExactly("id1", "id2");
            assertThatAssetHasCreatedAndLastModified(updatedAsset, "admin");
            assertThatDateAIsAfterDateB(updatedAsset.getLastModified(), asset.getLastModified());
        });
    }

    @Test
    public void shouldUpdateAssetsExternalIds() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            assertThat(asset.getAssetName()).isEqualTo("test");
            assertThat(asset.getExternalIds()).containsExactly("id1", "id2");
            assertThatAssetHasCreatedAndLastModified(asset, "admin");

            asset.setExternalIds(ImmutableList.of("id1", "id2", "id3", "id4"));

            Asset updatedAsset = contentManager.updateAsset(asset);
            assertThat(updatedAsset.getAssetName()).isEqualTo("test");
            assertThat(updatedAsset.getExternalIds()).containsExactly("id1", "id2", "id3", "id4");
            assertThatAssetHasCreatedAndLastModified(updatedAsset, "admin");
            assertThatDateAIsAfterDateB(updatedAsset.getLastModified(), asset.getLastModified());
        });
    }

    @Test
    public void shouldRemoveAssetExternalIds() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            assertThat(asset.getAssetName()).isEqualTo("test");
            assertThat(asset.getExternalIds()).containsExactly("id1", "id2");
            assertThatAssetHasCreatedAndLastModified(asset, "admin");

            asset.setExternalIds(new ArrayList<>());

            Asset updatedAsset = contentManager.updateAsset(asset);
            assertThat(updatedAsset.getAssetName()).isEqualTo("test");
            assertThat(updatedAsset.getExternalIds()).isEmpty();
            assertThatAssetHasCreatedAndLastModified(updatedAsset, "admin");
            assertThatDateAIsAfterDateB(updatedAsset.getLastModified(), asset.getLastModified());
        });
    }

    @Test
    public void shouldDeleteAsset() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            assertThat(contentManager.getAsset(asset.getId()).get()).isEqualToComparingFieldByField(asset);
            contentManager.deleteAsset(asset.getId());
            assertThat(contentManager.getAsset(asset.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldFailToDeleteAssetNotFound() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                contentManager.deleteAsset(idGenerator.getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotFoundException.class);
            }
        });
    }

    @Test
    public void shouldFailToDeleteAssetNotAuthorized() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            assertThat(contentManager.getAsset(asset.getId()).get()).isEqualToComparingFieldByField(asset);
        });
        integrationTests.destroyJCRSession();
        TestRunner.runAsUser("reader", "password", () -> {
            try {
                contentManager.deleteAsset(contentManager.findAssets(new DefaultSearchBuilder().build()).getRecords().iterator().next().getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotAuthorizedException.class);
            }
        });
    }

    @Test
    public void shouldFailToDeleteAssetWithVariants() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            contentManager.createVariant(asset.getId(), new Variant());

            try {
                contentManager.deleteAsset(asset.getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("One or more variants are associated to asset " + asset.getId());
            }
        });
    }

    @Test
    public void shouldCreateVariantWithRequiredFields() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant());
            assertThat(variant.getVariantName()).isNull();
            assertThat(variant.getExternalIds()).isEmpty();
            assertThatVariantHasCreatedAndLastModified(variant, "admin");
        });
    }

    @Test
    public void shouldCreateVariantWithAllFields() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));
            assertThat(variant.getVariantName()).isEqualTo("test variant");
            assertThat(variant.getExternalIds()).containsExactly("id1", "id2");
            assertThatVariantHasCreatedAndLastModified(variant, "admin");
        });
    }

    @Test
    public void shouldFailToCreateVariantNotAuthorized() throws Exception {
        TestRunner.runAsUser("admin", "password", this::createParentAsset);
        integrationTests.destroyJCRSession();
        TestRunner.runAsUser("reader", "password", () -> {
            try {
                contentManager.createVariant(contentManager.findAssets(new DefaultSearchBuilder().build()).getRecords().get(0).getId(), new Variant("test variant", ImmutableList.of()));
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotAuthorizedException.class);
            }
        });

    }

    @Test
    public void shouldGetVariants() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            contentManager.createVariant(asset.getId(), new Variant("test variant 1", ImmutableList.of()));
            contentManager.createVariant(asset.getId(), new Variant("test variant 2", ImmutableList.of()));
            contentManager.createVariant(asset.getId(), new Variant("test variant 3", ImmutableList.of()));
            SearchResult<Variant> searchResult = contentManager.findVariants(asset.getId(), new DefaultSearchBuilder().build());
            assertThat(searchResult.getTotalRecords()).isEqualTo(3);
            assertThat(searchResult.getRecords()).hasSize(3).extracting("variantName").containsOnly("test variant 1", "test variant 2", "test variant 3");
        });
    }

    @Test
    public void shouldGetVariantByAssetAndVariantId() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).get()).isEqualToComparingFieldByField(variant);
        });
    }

    @Test
    public void shouldGetVariantByVariantId() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            assertThat(contentManager.getVariant(variant.getId()).get()).isEqualToComparingFieldByField(variant);
        });
    }

    @Test
    public void shouldReturnEmptyOptionalForVariantIdThatDoesNotExist() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            assertThat(contentManager.getVariant(asset.getId(), "a").isPresent()).isFalse();
        });
    }

    @Test
    public void shouldReturnEmptyOptionalForBlankVariantId() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> assertThat(contentManager.getVariant("").isPresent()).isFalse());
    }

    @Test
    public void shouldUpdateVariantName() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            assertThat(variant.getVariantName()).isEqualTo("test variant");
            assertThat(variant.getExternalIds()).isEmpty();
            assertThatVariantHasCreatedAndLastModified(variant, "admin");

            variant.setVariantName("test variant 1");

            Variant updatedVariant = contentManager.updateVariant(variant);
            assertThat(updatedVariant.getVariantName()).isEqualTo("test variant 1");
            assertThat(updatedVariant.getExternalIds()).isEmpty();
            assertThatVariantHasCreatedAndLastModified(updatedVariant, "admin");
            assertThatDateAIsAfterDateB(updatedVariant.getLastModified(), variant.getLastModified());
        });
    }

    @Test
    public void shouldAddExternalIdsToVariant() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            assertThat(variant.getVariantName()).isEqualTo("test variant");
            assertThat(variant.getExternalIds()).isEmpty();
            assertThatVariantHasCreatedAndLastModified(variant, "admin");

            variant.setExternalIds(ImmutableList.of("id1", "id2"));

            Variant updatedVariant = contentManager.updateVariant(variant);
            assertThat(updatedVariant.getVariantName()).isEqualTo("test variant");
            assertThat(updatedVariant.getExternalIds()).containsExactly("id1", "id2");
            assertThatVariantHasCreatedAndLastModified(updatedVariant, "admin");
            assertThatDateAIsAfterDateB(updatedVariant.getLastModified(), variant.getLastModified());
        });
    }

    @Test
    public void shouldUpdateVariantExternalIds() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));
            assertThat(variant.getVariantName()).isEqualTo("test variant");
            assertThat(variant.getExternalIds()).containsExactly("id1", "id2");
            assertThatVariantHasCreatedAndLastModified(variant, "admin");

            variant.setExternalIds(ImmutableList.of("id1", "id2", "id3", "id4"));

            Variant updatedVariant = contentManager.updateVariant(variant);
            assertThat(updatedVariant.getVariantName()).isEqualTo("test variant");
            assertThat(updatedVariant.getExternalIds()).containsExactly("id1", "id2", "id3", "id4");
            assertThatVariantHasCreatedAndLastModified(updatedVariant, "admin");
            assertThatDateAIsAfterDateB(updatedVariant.getLastModified(), variant.getLastModified());
        });
    }

    @Test
    public void shouldRemoveVariantExternalIds() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));
            assertThat(variant.getVariantName()).isEqualTo("test variant");
            assertThat(variant.getExternalIds()).containsExactly("id1", "id2");
            assertThatVariantHasCreatedAndLastModified(variant, "admin");

            variant.setExternalIds(ImmutableList.of());

            Variant updatedVariant = contentManager.updateVariant(variant);
            assertThat(updatedVariant.getVariantName()).isEqualTo("test variant");
            assertThat(updatedVariant.getExternalIds()).isEmpty();
            assertThatVariantHasCreatedAndLastModified(updatedVariant, "admin");
            assertThatDateAIsAfterDateB(updatedVariant.getLastModified(), variant.getLastModified());
        });
    }

    @Test
    public void shouldDeleteVariantWithNoFiles() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).get()).isEqualToComparingFieldByField(variant);
            contentManager.deleteVariant(asset.getId(), variant.getId());
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldDeleteVariantSync() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));

            Repository repository = repositoryManager.createRepository(new Repository("test", "", "S3"));
            repositoryManager.updateRepositoryConfiguration(repository.getId(), new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", null));
            RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variant.getId(), repository.getId());
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();

            contentManager.createVariantFile(variant.getId(), newVariantFile());
            contentManager.associateVariantToRepositories(variant.getId(), ImmutableList.of(new VariantRepository(repository.getId(), repository.getRepositoryName(), repository.getType())));
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isTrue();
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);


            String jobId = contentManager.deleteVariant(asset.getId(), variant.getId()).get();
            waitUntilJobHasStatus(jobId, JobStatus.COMPLETED);
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).isPresent()).isFalse();
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldDeleteVariantAsync() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));

            Repository repository = repositoryManager.createRepository(new Repository("test", "", "FILE"));
            repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test"));
            RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variant.getId(), repository.getId());
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();

            contentManager.createVariantFile(variant.getId(), newVariantFile());
            contentManager.associateVariantToRepositories(variant.getId(), ImmutableList.of(new VariantRepository(repository.getId(), repository.getRepositoryName(), repository.getType())));
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isTrue();
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);


            String jobId = contentManager.deleteVariant(asset.getId(), variant.getId()).get();

            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).isPresent()).isFalse();
            // The underlying repository file should still be present
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();
            // The job should be queued
            assertThat(jobManager.getJob(jobId).get().getStatus()).isEqualTo(JobStatus.QUEUED);
        });
    }

    @Test
    public void shouldDeleteVariantFromRepositorySync() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));

            Repository repository = repositoryManager.createRepository(new Repository("test", "", "S3"));
            repositoryManager.updateRepositoryConfiguration(repository.getId(), new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", null));
            RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variant.getId(), repository.getId());
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();

            contentManager.createVariantFile(variant.getId(), newVariantFile());
            contentManager.associateVariantToRepositories(variant.getId(), ImmutableList.of(new VariantRepository(repository.getId(), repository.getRepositoryName(), repository.getType())));
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isTrue();
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);


            String jobId = contentManager.deleteVariantFromRepository(asset.getId(), variant.getId(), repository.getId());
            waitUntilJobHasStatus(jobId, JobStatus.COMPLETED);

            // The variant repository and files should be removed
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).isPresent()).isTrue();
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isFalse();
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(0);
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isFalse();
        });
    }

    @Test
    public void shouldDeleteVariantFromRepositoryAsync() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));

            Repository repository = repositoryManager.createRepository(new Repository("test", "", "FILE"));
            repositoryManager.updateRepositoryConfiguration(repository.getId(), new FileRepositoryConfiguration("/test"));
            RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variant.getId(), repository.getId());
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();

            contentManager.createVariantFile(variant.getId(), newVariantFile());
            contentManager.associateVariantToRepositories(variant.getId(), ImmutableList.of(new VariantRepository(repository.getId(), repository.getRepositoryName(), repository.getType())));
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isTrue();
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);


            String jobId = contentManager.deleteVariantFromRepository(asset.getId(), variant.getId(), repository.getId());

            // The variant repository and files should be removed
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).isPresent()).isTrue();
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isFalse();
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(0);
            // The underlying repository file should still be present
            assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();
            // The job should be queued
            assertThat(jobManager.getJob(jobId).get().getStatus()).isEqualTo(JobStatus.QUEUED);
        });
    }

    @Test
    public void shouldDeleteVariantFromRepositoryWhenStoredInMultipleRepositories() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));

            List<Repository> repositories = IntStream.range(0, 2).mapToObj(i -> {
                Repository repository = repositoryManager.createRepository(new Repository("test" + i, "", "S3"));
                repositoryManager.updateRepositoryConfiguration(repository.getId(), new S3RepositoryConfiguration("access", "secret", "us-west-1", "test", null));
                RepositoryFile repositoryFile = repositoryManager.createRepositoryFile(variant.getId(), repository.getId());
                assertThat(repositoryManager.getRepositoryFile(repository.getId(), repositoryFile.getId()).isPresent()).isTrue();
                return repository;
            }).collect(ImmutableListCollector.toImmutableList());

            contentManager.createVariantFile(variant.getId(), newVariantFile());

            repositories.forEach(repository -> {
                contentManager.associateVariantToRepositories(variant.getId(), ImmutableList.of(new VariantRepository(repository.getId(), repository.getRepositoryName(), repository.getType())));
                assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), repository.getId()).isPresent()).isTrue();
                assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);
            });

            assertThat(contentManager.findVariantRepositories(asset.getId(), variant.getId(), new VariantRepositorySearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(2);

            String jobId = contentManager.deleteVariantFromRepository(asset.getId(), variant.getId(), repositories.get(0).getId());
            waitUntilJobHasStatus(jobId, JobStatus.COMPLETED);

            // The variant should only be in one repo and the variant files should still exist
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).isPresent()).isTrue();
            assertThat(contentManager.findVariantRepositories(asset.getId(), variant.getId(), new VariantRepositorySearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);
            assertThat(contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().onlyCount(true).build()).getTotalRecords()).isEqualTo(1);
            assertThat(repositoryManager.getRepositoryFilesForVariant(repositories.get(0).getId(), variant.getId())).hasSize(0);
        });
    }

    @Test
    public void shouldFailToDeleteVariantNotFound() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            try {
                Asset asset = createParentAsset();
                contentManager.deleteVariant(asset.getId(), "a");
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotFoundException.class);
            }
        });
    }

    @Test
    public void shouldFailToDeleteVariantNotAuthorized() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of("id1", "id2")));
            assertThat(contentManager.getVariant(asset.getId(), variant.getId()).get()).isEqualToComparingFieldByField(variant);
        });
        integrationTests.destroyJCRSession();
        TestRunner.runAsUser("reader", "password", () -> {
            try {
                Search defaultSearch = new DefaultSearchBuilder().build();
                String assetId = contentManager.findAssets(defaultSearch).getRecords().get(0).getId();
                String variantId = contentManager.findVariants(assetId, defaultSearch).getRecords().get(0).getId();
                contentManager.deleteVariant(assetId, variantId);
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotAuthorizedException.class);
            }
        });
    }

    @Test
    public void shouldFailToDeleteVariantWithActiveJobs() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = contentManager.createAsset(new Asset("test", ImmutableList.of("id1", "id2")));
            Variant variant = contentManager.createVariant(asset.getId(), new Variant());
            Job job = createJobForVariant(variant);
            // needs to be QUEUED or EXECUTING in order to fail
            job.setStatus(JobStatus.EXECUTING);
            jobDAO.update(job);
            try {
                contentManager.deleteVariant(asset.getId(), variant.getId());
                failBecauseExceptionWasNotThrown(EJBException.class);
            } catch (EJBException e) {
                assertThat(e).hasCauseInstanceOf(NotUpdateableException.class);
                assertThat(e.getCause()).hasMessage("One or more active jobs are associated with variant " + variant.getId());
            }
        });
    }

    @Test
    public void shouldGetVariantRepositories() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant());
            VariantRepository variantRepository = new VariantRepository(idGenerator.getId(), "test", "FILE");
            variantRepository.setId(idGenerator.getId());
            variantRepositoryDAO.create(variant.getNodePath(), variantRepository);
            SearchResult<VariantRepository> searchResult = contentManager.findVariantRepositories(asset.getId(), variant.getId(), new VariantRepositorySearchBuilder().build());
            assertThat(searchResult.getTotalRecords()).isEqualTo(1);
            assertThat(searchResult.getRecords()).hasSize(1).extracting("repositoryName").containsOnly("test");
        });
    }

    @Test
    public void shouldReturnEmptySearchResultsWhenVariantHasNoRepositories() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant());
            SearchResult<VariantRepository> searchResult = contentManager.findVariantRepositories(asset.getId(), variant.getId(), new VariantRepositorySearchBuilder().build());
            assertThat(searchResult.getTotalRecords()).isEqualTo(0);
        });
    }

    @Test
    public void shouldGetVariantRepository() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            VariantRepository variantRepository = variantRepositoryDAO.create(variant.getNodePath(), new VariantRepository(idGenerator.getId(), "test", "FILE"));
            assertThat(contentManager.getVariantRepository(asset.getId(), variant.getId(), variantRepository.getId()).get()).isEqualToComparingFieldByField(variantRepository);
        });
    }

    @Test
    public void shouldGetVariantFiles() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant());
            VariantFile variantFile = contentManager.createVariantFile(variant.getId(), newVariantFile());
            SearchResult<VariantFile> searchResult = contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().build());
            assertThat(searchResult.getTotalRecords()).isEqualTo(1);
            assertThat(searchResult.getRecords()).hasSize(1);
            assertThat(searchResult.getRecords().get(0)).isEqualToIgnoringGivenFields(variantFile, "hashes");
            assertThat(searchResult.getRecords().get(0).getHashes().get(0)).isEqualToComparingFieldByField(variantFile.getHashes().get(0));
        });
    }

    @Test
    public void shouldGetVariantFile() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            VariantFile variantFile = contentManager.createVariantFile(variant.getId(), newVariantFile());
            VariantFile retrievedVariantFile = contentManager.getVariantFile(asset.getId(), variant.getId(), variantFile.getId()).get();
            assertThat(retrievedVariantFile).isEqualToIgnoringGivenFields(variantFile, "hashes");
            assertThat(retrievedVariantFile.getHashes().get(0)).isEqualToComparingFieldByField(variantFile.getHashes().get(0));
        });
    }

    @Test
    public void shouldCreateVariantFileWithChildren() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            VariantFile expected = new VariantFile("test");
            VariantFile actual = contentManager.createVariantFile(variant.getId(), new VariantFile("test"));
            assertThat(actual.getVariantFilename()).isEqualTo("test");

            contentManager.createChildVariantFile(variant.getId(), actual, newVariantFile());
            contentManager.createChildVariantFile(variant.getId(), actual, newVariantFile());
            assertThat(contentManager.getChildVariantFiles(variant.getId(), actual.getId())).hasSize(2);
        });
    }

    @Test
    public void shouldNotReturnChildVariantFilesWhenSearching() throws Exception {
        TestRunner.runAsUser("admin", "password", () -> {
            Asset asset = createParentAsset();
            Variant variant = contentManager.createVariant(asset.getId(), new Variant("test variant", ImmutableList.of()));
            VariantFile variantFile = contentManager.createVariantFile(variant.getId(), newVariantFile());

            VariantFile parent = contentManager.createVariantFile(variant.getId(), new VariantFile("test"));
            contentManager.createChildVariantFile(variant.getId(), parent, newVariantFile());
            contentManager.createChildVariantFile(variant.getId(), parent, newVariantFile());

            SearchResult<VariantFile> searchResult = contentManager.findVariantFiles(asset.getId(), variant.getId(), new VariantFileSearchBuilder().build());
            assertThat(searchResult.getTotalRecords()).isEqualTo(2);
            assertThat(searchResult.getRecords()).hasSize(2);
            assertThat(searchResult.getRecords()).extracting("id").containsOnly(variantFile.getId(), parent.getId());
        });
    }

    private void assertThatAssetHasCreatedAndLastModified(Asset asset, String expectedUser) {
        assertThat(asset.getCreated()).isNotNull();
        assertThat(asset.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(asset.getLastModified()).isNotNull();
        assertThat(asset.getLastModifiedBy()).isEqualTo(expectedUser);
    }

    private void assertThatVariantHasCreatedAndLastModified(Variant variant, String expectedUser) {
        assertThat(variant.getCreated()).isNotNull();
        assertThat(variant.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(variant.getLastModified()).isNotNull();
        assertThat(variant.getLastModifiedBy()).isEqualTo(expectedUser);
    }

    private void assertThatDateAIsAfterDateB(Date dateA, Date dateB) {
        assertThat(ZonedDateTime.ofInstant(dateA.toInstant(), ZoneId.systemDefault()).isAfter(ZonedDateTime.ofInstant(dateB.toInstant(), ZoneId.systemDefault()))).isTrue();
    }

    private Asset createParentAsset() {
        return contentManager.createAsset(new Asset("test", new ArrayList<>()));
    }

    private Job createJobForVariant(Variant variant) {
        Repository testRepository = createTestRepository();
        return jobManager.createJob(Job.Builder.build(j -> {
            j.setJobType(JobType.INGEST);
            j.setJobConfiguration(IngestJobConfiguration.Builder.build(c -> {
                c.setVariant(variant.getId());
                c.setRepositories(ImmutableList.of(testRepository.getId()));
                c.setIngestJobFiles(ImmutableList.of(IngestJobFile.Builder.build(ingestJobFile -> ingestJobFile.setUri("file://test"))));
            }));
        }));
    }

    private Repository createTestRepository() {
        Repository testRepository = repositoryManager.createRepository(new Repository("test", "", "FILE"));
        repositoryManager.updateRepositoryConfiguration(testRepository.getId(), new FileRepositoryConfiguration("/test"));
        return testRepository;
    }

    private VariantFile newVariantFile() {
        return new VariantFile("test.txt", 100L, "test.txt", "", ImmutableList.of(new VariantFileHash("abc", "MD5")));
    }

    private void waitUntilJobHasStatus(String jobId, JobStatus jobStatus) {
        for (int i = 0; i < 100; i++) {
            if (jobManager.getJob(jobId).get().getStatus().equals(jobStatus)) {
                break;
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}