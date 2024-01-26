/*
 * Copyright 2019-2024 Open Text.
 *
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
 */
package com.github.cafdataprocessing.worker.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * The test implementations are in a separate module for dependency analysis reasons.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DisplayName("IT for IngestionBatchWorkerPlugin")
public class BatchWorkerAcceptanceIT
{
    private final BatchWorkerAcceptanceTesting testing;

    public BatchWorkerAcceptanceIT() throws Exception
    {
        this.testing = new BatchWorkerAcceptanceTesting();
    }

    @Test
    @DisplayName("Check environmental variables for tests have been set")
    void checkEnvVariablesTest()
    {
        testing.checkEnvVariablesTest();
    }

    @Test
    @DisplayName("Check number of messages for single Subbatch")
    void countMessagesSingleSubbatchTest() throws Exception
    {
        testing.countMessagesSingleSubbatchTest();
    }

    @Test
    @DisplayName("Check number of messages for single Batch")
    void countMessagesSingleBatchTest() throws Exception
    {
        testing.countMessagesSingleBatchTest();
    }

    @Test
    @DisplayName("Check number of messages for multiple Batches")
    void countMessagesMultipleBatchesTest() throws Exception
    {
        testing.countMessagesMultipleBatchesTest();
    }

    @Test
    @DisplayName("Check number of messages for 937 subbatches")
    void countMessagesManySubbatchesTest() throws Exception
    {
        testing.countMessagesManySubbatchesTest();
    }

    @Test
    @DisplayName("Multple batches check custom data managed")
    void checkCustomDataMultipleBatchesTest() throws Exception
    {
        testing.checkCustomDataMultipleBatchesTest();
    }

    @Test
    @DisplayName("Multple batches check script managed")
    void checkScriptMultipleBatchesTest() throws Exception
    {
        testing.checkScriptMultipleBatchesTest();
    }

    @Test
    @DisplayName("Test data put in the TaskMessage and DocumentWorkerDocumentTask")
    void checkDataTest() throws Exception
    {
        testing.checkDataTest();
    }

    @Test
    @DisplayName("Test batch definitions is null")
    void batchDefinitionNullTest() throws Exception
    {
        testing.batchDefinitionNullTest();
    }

    @Test
    @DisplayName("Test invalid tenantId")
    void invalidTenantIdTest() throws Exception
    {
        testing.invalidTenantIdTest();
    }

    @Test
    @DisplayName("Test invalid batchId")
    void invalidBatchIdTest() throws Exception
    {
        testing.invalidBatchIdTest();
    }

    @Test
    @DisplayName("Test invalid json")
    void invalidJsonTest() throws Exception
    {
        testing.invalidJsonTest();
    }

    @Test
    @DisplayName("Test non existing batch file")
    void nonExistingFileTest() throws Exception
    {
        testing.nonExistingFileTest();
    }

    @Test
    @DisplayName("Test non existing batch directory")
    void nonExistingDirectoryTest() throws Exception
    {
        testing.nonExistingDirectoryTest();
    }

    @Test
    @DisplayName("Test non existing batch directory in multibatch")
    void nonExistingDirectoryInMultiBatchTest() throws Exception
    {
        testing.nonExistingDirectoryInMultiBatchTest();
    }

    @Test
    @DisplayName("Test non existing directory in subbatch")
    void nonExistingDirectoryInSubbatchTest() throws Exception
    {
        testing.nonExistingDirectoryInSubbatchTest();
    }

    @Test
    @DisplayName("Test tenantId not present")
    void tenantIdNotPresentTest() throws Exception
    {
        testing.tenantIdNotPresentTest();
    }
}
