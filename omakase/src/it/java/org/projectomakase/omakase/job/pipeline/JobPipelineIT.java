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
package org.projectomakase.omakase.job.pipeline;

import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * Transfer Pipeline (Ingest, Export, Replication) Integration Tests
 *
 * @author Richard Lucas
 */
@RunWith(Arquillian.class)
public class JobPipelineIT {

    @Inject
    IntegrationTests integrationTests;
    @Inject
    JobScenarioExecutor jobScenarioExecutor;


    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() throws Exception {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @After
    public void after() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    // INGEST

    @Test
    public void shouldIngestSingleFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 1).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantWithFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").failedIngestJob("repo1", 1, JobScenario.JobFailureType.TRANSFER).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantSuccessfullyIntoS3() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").ingestJob("repo1", 1).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantIntoS3WithHashingFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").failedIngestJob("repo1", 1, JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantIntoS3WithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").failedIngestJob("repo1", 1, JobScenario.JobFailureType.TRANSFER).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantSuccessfullyIntoGlacier() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").ingestJob("repo1", 1).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantIntoGlacierWithHashingFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").failedIngestJob("repo1", 1, JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldIngestSingleFileVariantIntoGlacierWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").failedIngestJob("repo1", 1, JobScenario.JobFailureType.TRANSFER).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 2).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantWithFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").failedIngestJob("repo1", 2, JobScenario.JobFailureType.TRANSFER).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantSuccessfullyIntoS3() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").ingestJob("repo1", 2).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantIntoS3WithHashingFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").failedIngestJob("repo1", 2, JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantIntoS3WithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").failedIngestJob("repo1", 2, JobScenario.JobFailureType.TRANSFER).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantSuccessfullyIntoGlacier() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").ingestJob("repo1", 2).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantIntoGlacierWithHashingFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").failedIngestJob("repo1", 2, JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldIngestMultiFileVariantIntoGlacierWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").failedIngestJob("repo1", 2, JobScenario.JobFailureType.TRANSFER).build()));
    }

    // HLS INGEST

    @Test
    public void shouldIngestHLSMediaVariantSuccessfully() {
        // simple HLS media playlist that references ts chunks
        TestRunner
                .runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").hlsIngestJob("repo1", JobScenario.HLSManifestType.MEDIA).build()));
    }

    @Test
    public void shouldIngestHLSMasterVariantSuccessfully() {
        // simple HLS media playlist that references ts chunks
        TestRunner
                .runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").hlsIngestJob("repo1", JobScenario.HLSManifestType.MASTER).build()));
    }

    // EXPORT

    @Test
    public void shouldExportSingleFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 1).exportJob("repo1").build()));
    }

    @Test
    public void shouldExportSingleFileVariantThatRequiresRestore() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").ingestJob("repo1", 1).exportJob("repo1").build()));
    }

    @Test
    public void shouldExportSingleFileVariantWithRestoreFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").ingestJob("repo1", 1).failedExportJob("repo1", JobScenario.JobFailureType.RESTORE).build()));
    }

    @Test
    public void shouldExportSingleFileVariantWithTransferFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 1).failedExportJob("repo1", JobScenario.JobFailureType.TRANSFER).build()));
    }

    @Test
    public void shouldExportMultiFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 2).exportJob("repo1").build()));
    }

    @Test
    public void shouldExportMultiFileVariantThatRequiresRestore() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").ingestJob("repo1", 2).exportJob("repo1").build()));
    }

    @Test
    public void shouldExportMultiFileVariantWithRestoreFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "GLACIER").ingestJob("repo1", 2).failedExportJob("repo1", JobScenario.JobFailureType.RESTORE).build()));
    }

    @Test
    public void shouldExportMultiFileVariantWithTransferFailure() {
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 2).failedExportJob("repo1", JobScenario.JobFailureType.TRANSFER).build()));
    }

    // HLS EXPORT

    @Test
    public void shouldExportHLSMediaVariantSuccessfully() {
        // simple HLS media playlist that references ts chunks
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").hlsIngestJob("repo1", JobScenario.HLSManifestType.MEDIA).hlsExportJob("repo1").build()));
    }

    @Test
    public void shouldExportHLSMasterVariantSuccessfully() {
        // simple HLS media playlist that references ts chunks
        TestRunner.runAsUser("admin", "password",
                () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").hlsIngestJob("repo1", JobScenario.HLSManifestType.MASTER).hlsExportJob("repo1").build()));
    }

    // REPLICATION

    @Test
    public void shouldReplicateSingleFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "FILE").repository("destination", "FILE").ingestJob("source", 1).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "FILE").repository("destination", "FILE").ingestJob("source", 1).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantToS3Successfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "FILE").repository("destination", "S3").ingestJob("source", 1).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantToS3WithHashingFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("source", "FILE").repository("destination", "S3").ingestJob("source", 1)
                .failedReplicationJob("source", "destination", JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantToS3WithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "FILE").repository("destination", "S3").ingestJob("source", 1).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantToGlacierSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "FILE").repository("destination", "GLACIER").ingestJob("source", 1).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantToGlacierWithHashingFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("source", "FILE").repository("destination", "GLACIER").ingestJob("source", 1)
                .failedReplicationJob("source", "destination", JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantToGlacierWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "FILE").repository("destination", "GLACIER").ingestJob("source", 1).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantFromS3Successfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "S3").repository("destination", "FILE").ingestJob("source", 1).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantFromS3WithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "S3").repository("destination", "FILE").ingestJob("source", 1).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantFromGlacierSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "GLACIER").repository("destination", "FILE").ingestJob("source", 1).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantFromGlacierWithRestoreFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "GLACIER").repository("destination", "FILE").ingestJob("source", 1).failedReplicationJob("source", "destination", JobScenario.JobFailureType.RESTORE)
                        .build()));
    }

    @Test
    public void shouldReplicateSingleFileVariantFromGlacierWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "GLACIER").repository("destination", "FILE").ingestJob("source", 1).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "FILE").repository("destination", "FILE").ingestJob("source", 2).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "FILE").repository("destination", "FILE").ingestJob("source", 2).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantToS3Successfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "FILE").repository("destination", "S3").ingestJob("source", 2).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantToS3WithHashingFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("source", "FILE").repository("destination", "S3").ingestJob("source", 2)
                .failedReplicationJob("source", "destination", JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantToS3WithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "FILE").repository("destination", "S3").ingestJob("source", 2).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantToGlacierSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "FILE").repository("destination", "GLACIER").ingestJob("source", 2).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantToGlacierWithHashingFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("source", "FILE").repository("destination", "GLACIER").ingestJob("source", 2)
                .failedReplicationJob("source", "destination", JobScenario.JobFailureType.MULTIPART_PREPARE).build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantToGlacierWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "FILE").repository("destination", "GLACIER").ingestJob("source", 2).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantFromS3Successfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "S3").repository("destination", "FILE").ingestJob("source", 2).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantFromS3WithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "S3").repository("destination", "FILE").ingestJob("source", 2).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantFromGlacierSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor
                .execute(JobScenario.builder().repository("source", "GLACIER").repository("destination", "FILE").ingestJob("source", 2).replicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantFromGlacierWithRestoreFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "GLACIER").repository("destination", "FILE").ingestJob("source", 2).failedReplicationJob("source", "destination", JobScenario.JobFailureType.RESTORE)
                        .build()));
    }

    @Test
    public void shouldReplicateMultiFileVariantFromGlacierWithTransferFailure() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                JobScenario.builder().repository("source", "GLACIER").repository("destination", "FILE").ingestJob("source", 2).failedReplicationJob("source", "destination", JobScenario.JobFailureType.TRANSFER)
                        .build()));
    }

    // HLS REPLICATIONS

    @Test
    public void shouldReplicateHLSMediaVariantSuccessfully() {
        // simple HLS media playlist that references ts chunks
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                        JobScenario.builder().repository("source", "FILE").repository("destination", "FILE").hlsIngestJob("source", JobScenario.HLSManifestType.MEDIA)
                                .hlsReplicationJob("source", "destination").build()));
    }

    @Test
    public void shouldReplicateHLSMasterVariantSuccessfully() {
        // simple HLS media playlist that references ts chunks
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(
                        JobScenario.builder().repository("source", "FILE").repository("destination", "FILE").hlsIngestJob("source", JobScenario.HLSManifestType.MASTER)
                                .hlsReplicationJob("source", "destination").build()));
    }

    // DELETE

    @Test
    public void shouldDeleteSingleFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 1).deleteJob().build()));
    }

    @Test
    public void shouldDeleteMultiFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").ingestJob("repo1", 2).deleteJob().build()));
    }

    @Test
    public void shouldDeleteHLSVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "FILE").hlsIngestJob("repo1", JobScenario.HLSManifestType.MASTER).deleteJob().build()));
    }

    @Test
    public void shouldRemoteDeleteSingleFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").ingestJob("repo1", 1).deleteJob().build()));
    }

    @Test
    public void shouldRemoteDeleteMultiFileVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").ingestJob("repo1", 2).deleteJob().build()));
    }

    @Test
    public void shouldRemoteDeleteHLSVariantSuccessfully() {
        TestRunner.runAsUser("admin", "password", () -> jobScenarioExecutor.execute(JobScenario.builder().repository("repo1", "S3").hlsIngestJob("repo1", JobScenario.HLSManifestType.MASTER).deleteJob().build()));
    }
}
