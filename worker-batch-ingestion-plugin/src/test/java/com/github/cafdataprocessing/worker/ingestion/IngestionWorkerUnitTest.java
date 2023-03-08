/*
 * Copyright 2019-2022 Micro Focus or one of its affiliates.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hpe.caf.messagebuilder.TaskMessage;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.batch.BatchWorkerTransientException;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DisplayName("Unit tests for the IngestionBatchWorkerPlugin")
public final class IngestionWorkerUnitTest
{
    private BatchWorkerServices testWorkerServices;
    private String taskMessageType;
    private Map<String, String> testTaskMessageParams;
    private EnvironmentVariables envVars;

    @BeforeEach
    public void clearEnvironmentVariables()
    {
        envVars = new EnvironmentVariables();
        envVars.set("CAF_VALIDATION_FILE", null);
    }

    @Test
    @DisplayName("Worker Instantiated if CAF_STAGING_SERVICE_BASEPATH is set")
    void testEnvVariable()
    {
        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        assertThat(plugin, is(notNullValue()));
    }

    @Test
    @RepeatedTest(19)
    @DisplayName("Test multiple batch ids successfully proccessed with custom data")
    void testMultiBatchIdsWithCustomData() throws JsonProcessingException, BatchDefinitionException, BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:ALPHA", "123456"));
        final String batchDefinition = "tenant1/batch1|batch2|batch3";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(35)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            final String returnedContentFieldDocumentWorkerFieldValueData = returnedTaskData.customData.get("ALPHA");

            assertThat(returnedContentFieldDocumentWorkerFieldValueData, is(equalTo("123456")));
        }
    }

    @Test
    @RepeatedTest(19)
    @DisplayName("Test multiple batch ids successfully processed without custom data")
    void testMultiBatchIdsWithoutCustomData() throws JsonProcessingException, BatchDefinitionException, BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinition = "tenant1/batch1|batch2|batch3";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(35)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            assertThat(returnedTaskData.customData.toString(), is(equalTo("{}")));
        }
    }

    @Test
    @RepeatedTest(19)
    @DisplayName("Test single batch id successfully processed with custom data")
    void testSingleBatchIdWithCustomData() throws JsonProcessingException, BatchDefinitionException, BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:BETA", "AZ9089"));
        final String batchDefinition = "tenant2/batch1";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(10)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            final String returnedContentFieldDocumentWorkerFieldValueData = returnedTaskData.customData.get("BETA");

            assertThat(returnedContentFieldDocumentWorkerFieldValueData, is(equalTo("AZ9089")));
        }
    }

    @Test
    @RepeatedTest(19)
    @DisplayName("Test single batch id successfully processed without custom data")
    void testSingleBatchIdWithoutCustomData() throws JsonProcessingException, BatchDefinitionException, BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinition = "tenant2/batch1";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(10)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            assertThat(returnedTaskData.customData.toString(), is(equalTo("{}")));
        }
    }

    @Test
    @RepeatedTest(19)
    @DisplayName("Test subbatch successfully processed with custom data")
    void testSubbatchWithCustomData() throws JsonProcessingException, BatchDefinitionException, BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final String batchDefinition = "subbatch:tenant3/batch3/20190328-100001-t04-json.batch";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(1)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            final String returnedContentFieldDocumentWorkerFieldValueData = returnedTaskData.customData.get("GAMMA");

            assertThat(returnedContentFieldDocumentWorkerFieldValueData, is(equalTo("MIOP90")));
        }
    }

    @Test
    @RepeatedTest(19)
    @DisplayName("Test subbatch successfully processed without custom data")
    void testSubbatchWithoutCustomData() throws JsonProcessingException, BatchDefinitionException, BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinition = "subbatch:tenant3/batch3/20190328-100001-t04-json.batch";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(1)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            assertThat(returnedTaskData.customData.toString(), is(equalTo("{}")));
        }
    }

    @ParameterizedTest
    @DisplayName("Test multiple batches with scripts")
    @MethodSource("scriptProvider")
    void testMultipleBatchesWithScripts(final Map<String, String> testTaskMessageParams) throws BatchDefinitionException,
                                                                                                BatchWorkerTransientException
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        final String batchDefinition = "subbatch:tenant3/batch3/20190328-100001-t04-json.batch";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        // Verify that expected number of messages were registered
        assertThat(constructedMessages.size(), is(equalTo(1)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            assertThat(returnedTaskData, is(notNullValue()));

            final int numberOfScripts = returnedTaskData.scripts.size();

            assertThat(numberOfScripts, is(equalTo(1)));
            assertThat(returnedTaskData.scripts.get(0).engine, is(equalTo("GRAAL_JS")));

            if (returnedTaskData.scripts.get(0).name.equals("workflow.js")) {
                assertThat(returnedTaskData.scripts.get(0).script, is(equalTo("/Scripts/WorkflowScript.js")));
            }
        }
    }

    @Test
    @DisplayName("Test batch definitions is null")
    void testBatchDefinitionsNull()
    {
        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        final Exception ex = assertThrows(BatchDefinitionException.class,
                                          () -> plugin.processBatch(testWorkerServices, null, taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), is(equalTo("IngestionBatchWorkerPlugin has not received a valid batch definition string")));
    }

    @Test
    @DisplayName("Test invalid batch id and tenant id with single batch")
    void testInvalidBatchIdAndTenantIdSingleBatch()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionInvalidBatch = "tenant2/bat:ch1";
        taskMessageType = "DocumentMessage";

        Exception ex = assertThrows(BatchDefinitionException.class,
                                    () -> plugin.processBatch(testWorkerServices, batchDefinitionInvalidBatch,
                                                              taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Invalid format of the batch definition: tenant2/bat:ch1"));

        final String batchDefinitionInvalidTenant = "tenant:2/batch1";
        ex = assertThrows(BatchDefinitionException.class,
                          () -> plugin.processBatch(testWorkerServices, batchDefinitionInvalidTenant,
                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Invalid format of the batch definition: tenant:2/batch1"));
    }

    @Test
    @DisplayName("Test invalid json")
    void testInvalidJson()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinition = "subbatch:tenant4/batch8/20190314-100001-t04-json.batch";
        taskMessageType = "DocumentMessage";

        final Exception ex = assertThrows(RuntimeException.class,
                                          () -> plugin.processBatch(testWorkerServices, batchDefinition,
                                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Exception while deserializing the json of"));
        assertThat(ex.getMessage(), containsString("20190314-100001-t04-json.batch"));
    }

    @Test
    @DisplayName("Test non existing json file")
    void testNonExistingFile()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionNonExistingFile = "subbatch:tenant4/batch8/20190314-100001-ttt-json.batch";
        taskMessageType = "DocumentMessage";

        final Exception ex = assertThrows(BatchDefinitionException.class,
                                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNonExistingFile,
                                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Exception while reading subbatch: "));
        assertThat(ex.getMessage(), containsString("20190314-100001-ttt-json.batch, it does not exist"));
    }

    @Test
    @DisplayName("Test non existing directory (batch)")
    void testNonExistingDirectory()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionNonExistingDirectory = "tenant4/batch10";
        taskMessageType = "DocumentMessage";

        final Exception ex = assertThrows(BatchDefinitionException.class,
                                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNonExistingDirectory,
                                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Exception while reading the batch: "));
        assertThat(ex.getMessage(), containsString("batch10 was not found"));
    }

    @Test
    @DisplayName("Test non existing directory (batch) in the a multiple batch")
    void testNonExistingDirectoryInMultiBatch()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionNonExistingDirectory = "tenant1/batch1|batch10|batch2";
        taskMessageType = "DocumentMessage";

        final Exception ex = assertThrows(Exception.class,
                                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNonExistingDirectory,
                                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Exception while reading the batch: "));
        assertThat(ex.getMessage(), containsString("batch10 was not found"));
    }

    @Test
    @DisplayName("Test non existing directory (batch) in a subbatch")
    void testNonExistingDirectoryInSubbatch()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionNonExistingDirectory = "subbatch:tenant4/batch10/20190314-100001-t04-json.batch";
        taskMessageType = "DocumentMessage";

        final Exception ex = assertThrows(BatchDefinitionException.class,
                                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNonExistingDirectory,
                                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Exception while reading subbatch: "));
        assertThat(ex.getMessage(), containsString(", it does not exist"));
    }

    @Test
    @DisplayName("Test transient exception")
    void testTransientException()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionNonExistingFile = "subbatch:tenant5/batch9/20190328-100001-t04-json.batch";
        taskMessageType = "DocumentMessage";

        final Exception ex = assertThrows(BatchWorkerTransientException.class,
                                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNonExistingFile,
                                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Transient exception while reading subbatch: "));
        assertThat(ex.getMessage(), containsString("20190328-100001-t04-json.batch, message: Input length = 2"));
    }

    @Test
    @DisplayName("Test tenantId not present in input string")
    void testTenantIdNotPresent()
    {
        final List<TaskMessage> constructedMessages = new ArrayList<>();

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = null;
        final String batchDefinitionNoTenantId = "batch8|batch5";
        taskMessageType = "DocumentMessage";

        Exception ex = assertThrows(BatchDefinitionException.class,
                                    () -> plugin.processBatch(testWorkerServices, batchDefinitionNoTenantId,
                                                              taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("The tenant id is not present in the string passed"));

        final String batchDefinitionNoTenantIdTwo = "/batch8|batch6";
        ex = assertThrows(BatchDefinitionException.class,
                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNoTenantIdTwo,
                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("The tenant id is not present in the string passed"));

        final String batchDefinitionNoBatchId = "tenant/";
        ex = assertThrows(BatchDefinitionException.class,
                          () -> plugin.processBatch(testWorkerServices, batchDefinitionNoBatchId,
                                                    taskMessageType, testTaskMessageParams));
        assertThat(ex.getMessage(), containsString("Exception while handling a single batch id: "));
    }

    @Test
    @DisplayName("Test validator with validation file and single batch")
    void testFieldValidator() throws BatchDefinitionException, BatchWorkerTransientException
    {
        final String agentTestFile = "target/test-classes/validator/agentFields-test3.json";
        final List<TaskMessage> constructedMessages = new ArrayList<>();
        final int expectedDocumentFailures = 2;
        envVars.set("CAF_VALIDATION_FILE", agentTestFile);

        final IngestionBatchWorkerPlugin plugin = new IngestionBatchWorkerPlugin();
        testWorkerServices = createTestBatchWorkerServices(constructedMessages, plugin);

        testTaskMessageParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:ALPHA", "123456"));
        final String batchDefinition = "tenant6/batch10";
        taskMessageType = "DocumentMessage";

        plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);

        assertThat(constructedMessages.size(), is(equalTo(1)));
        for (final TaskMessage returnedMessage : constructedMessages) {
            checkClassifierAndApiVersion(returnedMessage);

            final DocumentWorkerDocumentTask returnedTaskData = (DocumentWorkerDocumentTask) returnedMessage.getTaskData();
            final DocumentWorkerDocument returnedDocument = returnedTaskData.document;

            assertTrue(returnedDocument.fields.containsKey("OCR_0_0_NAME"));
            assertEquals(expectedDocumentFailures, returnedDocument.failures.size());
        }
    }

    @Test
    @DisplayName("Test validator with invalid validation file")
    void testFieldValidatorInvalidFile()
    {
        EnvironmentVariables envVars = new EnvironmentVariables();
        envVars.set("CAF_VALIDATION_FILE", "INVALID_TEST_FILE_PATH");

        final Exception exception = assertThrows(RuntimeException.class, IngestionBatchWorkerPlugin::new);

        final String expectedMessage = "Exception when attempting to read validation file" + "\n"
            + "ValidationFileAdapterException: Failed to get file contents from INVALID_TEST_FILE_PATH";

        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Stream<Arguments> scriptProvider()
    {
        return Stream.of(arguments(createTaskMessageParams(
            new AbstractMap.SimpleEntry<>("graaljs:resetDocumentOnError.js",
                                          "function onError(document, error) { document.getField('ERROR').add(error); }"))),
                         arguments(createTaskMessageParams(
                             new AbstractMap.SimpleEntry<>("graaljs:workflow.js", "/Scripts/WorkflowScript.js"))),
                         arguments(createTaskMessageParams(
                             new AbstractMap.SimpleEntry<>("graaljs:trackDocuments.js", "http://scriptserver/trackDocuments.js"))));
    }

    private BatchWorkerServices createTestBatchWorkerServices(final List<TaskMessage> constructedMessages,
                                                              final IngestionBatchWorkerPlugin plugin)
    {
        return new BatchWorkerServices()
        {
            @Override
            public void registerBatchSubtask(String batchDefinition)
            {
                try {
                    log.info("I am registering this subtask: " + batchDefinition);
                    plugin.processBatch(testWorkerServices, batchDefinition, taskMessageType, testTaskMessageParams);
                } catch (BatchDefinitionException | BatchWorkerTransientException e) {
                    throw new RuntimeException(e.getMessage(), e.getCause());
                }
            }

            @Override
            public void registerItemSubtask(final String taskClassifier, final int taskApiVersion, final Object taskData)
            {
                // Store this as a task message so we can refer to it when verifying results
                TaskMessage message = new TaskMessage();
                message.setTaskApiVersion(taskApiVersion);
                message.setTaskClassifier(taskClassifier);
                message.setTaskData(taskData);
                constructedMessages.add(message);
            }

            @Override
            public <S> S getService(final Class<S> aClass)
            {
                return null;
            }
        };
    }

    @SafeVarargs
    private static Map<String, String> createTaskMessageParams(Map.Entry<String, String>... entries)
    {
        final Map<String, String> testTaskMessageParams = new HashMap<>();
        for (final Map.Entry<String, String> entry : entries) {
            testTaskMessageParams.put(entry.getKey(), entry.getValue());
        }
        return testTaskMessageParams;
    }

    private void checkClassifierAndApiVersion(final TaskMessage returnedMessage)
    {
        assertThat(returnedMessage.getTaskApiVersion(), is(equalTo(DocumentWorkerConstants.WORKER_API_VER)));
        assertThat(returnedMessage.getTaskClassifier(), is(equalTo(DocumentWorkerConstants.DOCUMENT_TASK_NAME)));
    }
}
