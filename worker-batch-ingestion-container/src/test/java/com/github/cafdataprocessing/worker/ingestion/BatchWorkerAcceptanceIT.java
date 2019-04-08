/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.ApiException;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.Job;
import com.hpe.caf.services.job.client.model.NewJob;
import com.hpe.caf.services.job.client.model.WorkerAction;
import com.hpe.caf.worker.batch.BatchWorkerTask;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;


/*
 * Copyright 2019-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DisplayName("IT for IngestionBatchWorkerPlugin")
@Slf4j
public class BatchWorkerAcceptanceIT
{

    private final Connection conn;
    private final Channel channel;
    private final String workflow_queue;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JobsApi jobsApi;
    private final String output_queue;

    public BatchWorkerAcceptanceIT() throws IOException, TimeoutException
    {
        workflow_queue = "worker-workflow";
        output_queue = System.getenv("CAF_BATCH_WORKER_ERROR_QUEUE");
        log.debug("Output queue: " + output_queue);
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv("RABBITMQ_TEST_HOST"));
        factory.setPort(Integer.parseInt(System.getenv("RABBITMQ_TEST_PORT")));
        conn = factory.newConnection();
        channel = conn.createChannel();
        final ApiClient client = new ApiClient();
        client.setBasePath(System.getenv("JOB_SERVICE_ADDRESS"));
        jobsApi = new JobsApi(client);
    }

    @Test
    @DisplayName("Check environmental variables for tests have been set")
    void checkEnvVariablesTest() throws ApiException
    {
        log.debug("Job Service: " + System.getenv("JOB_SERVICE_ADDRESS"));
        assertThat(System.getenv("JOB_SERVICE_ADDRESS"), is(notNullValue()));
        log.debug("RabbitMQ host: " + System.getenv("RABBITMQ_TEST_HOST"));
        assertThat(System.getenv("RABBITMQ_TEST_HOST"), is(notNullValue()));
        log.debug("RabbitMQ port: " + System.getenv("RABBITMQ_TEST_PORT"));
        assertThat(System.getenv("RABBITMQ_TEST_PORT"), is(notNullValue()));
        assertThat(System.getenv("CAF_BATCH_WORKER_ERROR_QUEUE"), is(notNullValue()));
    }

    @Test
    @DisplayName("Check number of messages for single Subbatch")
    void countMessagesSingleSubbatchTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final NewJob job = createNewJobs("First Job", "Single subbatch",
                                         "subbatch:tenant3/batch3/20190328-100001-t04-json.batch", new HashMap<>());
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        // give time to the various apps to manage the job
        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);

        //ensure that an explicit ack is sent from worker before removing from the queue
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (true) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery(60000L);
            if (delivery == null) {
                break;
            }
            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);

        assertThat(count, is(equalTo(1)));
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Check number of messages for single Batch")
    void countMessagesSingleBatchTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final NewJob job = createNewJobs("Second Job", "Single batch", "tenant2/batch1", new HashMap<>());
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);
        //ensure that an explicit ack is sent from worker before removing from the queue
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (true) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery(60000L);
            if (delivery == null) {
                break;
            }
            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);

        assertThat(count, is(equalTo(10)));
        assertThat(messageCount, is(equalTo(10)));
    }

    @Test
    @DisplayName("Check number of messages for multiple Batches")
    void countMessagesMultipleBatchesTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant1/batch1|batch2|batch3", new HashMap<>());
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);
        //ensure that an explicit ack is sent from worker before removing from the queue
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (true) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery(60000L);
            if (delivery == null) {
                break;
            }
            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);

        assertThat(count, is(equalTo(35)));
        assertThat(messageCount, is(equalTo(35)));
    }

    @Test
    @DisplayName("Check number of messages for 937 subbatches")
    void countMessagesManySubbatchesTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final NewJob job = createNewJobs("Last Job", "Multiple batches", "tenant5/batchBig", new HashMap<>());
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (true) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery(60000L);
            if (delivery == null) {
                break;
            }
            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);

        assertThat(count, is(equalTo(937)));
        assertThat(messageCount, is(equalTo(937)));
    }

    @Test
    @DisplayName("Multple batches check custom data managed")
    void checkCustomDataMultipleBatchesTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant1/batch1|batch2|batch3", taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (count < 35) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));

            final byte[] taskData = returnedTaskData.getTaskData();
            final DocumentWorkerDocumentTask taskDecoded = mapper.readValue(new String(taskData), DocumentWorkerDocumentTask.class);

            final String returnedContentFieldDocumentWorkerFieldValueData = taskDecoded.customData.get("GAMMA");

            assertThat(returnedContentFieldDocumentWorkerFieldValueData, is(equalTo("MIOP90")));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);

        assertThat(messageCount, is(equalTo(35)));
    }

    @Test
    @DisplayName("Multple batches check script managed")
    void checkScriptMultipleBatchesTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("scripts:resetDocumentOnError.js",
                                                                                                     "function onError(document, error) "
                                                                                                     + "{ document.getField('ERROR')"
                                                                                                     + ".add(error); }"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant1/batch1|batch2|batch3", taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (count < 35) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));

            final byte[] taskData = returnedTaskData.getTaskData();
            final DocumentWorkerDocumentTask taskDecoded = mapper.readValue(new String(taskData), DocumentWorkerDocumentTask.class);

            assertThat(taskDecoded.scripts.size(), is(equalTo(1)));
            assertThat(taskDecoded.scripts.get(0).name, is(equalTo("resetDocumentOnError.js")));
            assertThat(taskDecoded.scripts.get(0).script,
                       is(equalTo("function onError(document, error) { document.getField('ERROR').add(error); }")));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);

        assertThat(messageCount, is(equalTo(35)));
    }

    @Test
    @DisplayName("Test data put in the TaskMessage and DocumentWorkerDocumentTask")
    void checkDataTest() throws ApiException, IOException, TimeoutException, InterruptedException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant1/batch1|batch2|batch3", taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(workflow_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(workflow_queue, autoAck, consumer);

        int count = 0;
        while (count < 35) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("DocumentWorkerTask")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.NEW_TASK)));
            assertThat(returnedTaskData.getTo(), is(equalTo(workflow_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final DocumentWorkerDocumentTask taskDecoded = mapper.readValue(new String(taskData), DocumentWorkerDocumentTask.class);

            final String returnedContentFieldDocumentWorkerFieldValueData = taskDecoded.customData.get("GAMMA");

            assertThat(returnedContentFieldDocumentWorkerFieldValueData, is(equalTo("MIOP90")));
            if (taskDecoded.document.reference.equals("thisIsTheOne")) {
                assertThat(taskDecoded.document.fields.get("FILE_SIZE").get(0).data, is(equalTo(10001)));
                assertThat(taskDecoded.changeLog.get(2).changes.get(1).setFields.get("tag").get(0).data, is(equalTo("taggone")));
                assertThat(taskDecoded.changeLog.get(2).changes.get(1).addFields.get("CAF_WORKFLOW_ACTIONS_COMPLETED").get(0).data,
                           is(equalTo(5)));
            }

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, workflow_queue);
        assertThat(messageCount, is(equalTo(35)));
    }

    @Test
    @DisplayName("Test batch definitions is null")
    void batchDefinitionNullTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", null, taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("IngestionBatchWorkerPlugin has not received a valid batch definition string"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test invalid tenantId")
    void invalidTenantIdTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant:1/batch1", taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("BatchDefinitionException Invalid format of the batch definition: tenant:1/batch1"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test invalid batchId")
    void invalidBatchIdTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant1/bat:ch1", taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("BatchDefinitionException Invalid format of the batch definition: tenant1/bat:ch1"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test invalid json")
    void invalidJsonTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "subbatch:tenant4/batch8/20190314-100001-t04-json.batch",
                                         taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("Exception while deserializing the json"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test non existing batch file")
    void nonExistingFileTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "subbatch:tenant4/batch8/20190314-100001-ttt-json.batch",
                                         taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("BatchDefinitionException Exception while reading subbatch: "
                       + "/srv/common/webdav/tenant4/completed/batch8/20190314-100001-ttt-json.batch, the file (or its folder) "
                       + "does not exist"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test non existing batch directory")
    void nonExistingDirectoryTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant4/batch10",
                                         taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("BatchDefinitionException Exception while reading the batch folder: "
                       + "/srv/common/webdav/tenant4/completed/batch10, the directory does not exist"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test non existing batch directory in multibatch")
    void nonExistingDirectoryInMultiBatchTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "tenant1/batch1|batch10|batch2",
                                         taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("Exception while reading the batch folder: "
                       + "/srv/common/webdav/tenant1/completed/batch10, the directory does not exist"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test non existing directory in subbatch")
    void nonExistingDirectoryInSubbatchTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "subbatch:tenant4/batch10/20190314-100001-t04-json.batch",
                                         taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("Exception while reading subbatch: "
                       + "/srv/common/webdav/tenant4/completed/batch10/20190314-100001-t04-json.batch, the file (or its folder) "
                       + "does not exist"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    @Test
    @DisplayName("Test tenantId not present")
    void tenantIdNotPresentTest() throws ApiException, InterruptedException, IOException
    {
        final String jobId = RandomStringUtils.randomAlphanumeric(10);
        final Map<String, String> taskParams = createTaskMessageParams(new AbstractMap.SimpleEntry<>("customdata:GAMMA", "MIOP90"));
        final NewJob job = createNewJobs("Third Job", "Multiple batches", "batch8|batch5", taskParams);
        jobsApi.createOrUpdateJob(jobId, job, "");
        final Job jobRetrieved = jobsApi.getJob(jobId, "");

        assertThat(jobRetrieved.getDescription(), is(equalTo(job.getDescription())));
        assertThat(jobRetrieved.getId(), is(equalTo(jobId)));

        Thread.sleep(30000L);

        final int messageCount = setupQueue(output_queue);
        final boolean autoAck = false;
        final QueueingConsumer consumer = new QueueingConsumer(channel);
        final String consumerTag = channel.basicConsume(output_queue, autoAck, consumer);

        int count = 0;
        while (count < 1) {
            final QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            final String message = new String(delivery.getBody());
            final TaskMessage returnedTaskData = mapper.readValue(message, TaskMessage.class);

            assertThat(returnedTaskData, is(notNullValue()));
            assertThat(returnedTaskData.getVersion(), is(equalTo(3)));
            assertThat(returnedTaskData.getTaskClassifier(), is(equalTo("BatchWorker")));
            assertThat(returnedTaskData.getTaskApiVersion(), is(equalTo(1)));
            assertThat(returnedTaskData.getTaskStatus(), is(equalTo(TaskStatus.RESULT_EXCEPTION)));
            assertThat(returnedTaskData.getTo(), is(equalTo(output_queue)));

            final byte[] taskData = returnedTaskData.getTaskData();
            final String exception = new String(taskData);

            assertThat(exception, containsString("The tenant id is not present in the string passed"));

            count++;
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        closeQueue(consumerTag, output_queue);
        assertThat(messageCount, is(equalTo(1)));
    }

    private NewJob createNewJobs(final String name, final String description, final String batchDefinition,
                                 final Map<String, String> taskMessageParams)
    {
        final NewJob job = new NewJob();
        job.setName(name);
        job.setDescription(description);
        final WorkerAction task = new WorkerAction();
        task.setTaskPipe("ingestion-batch-in");
        task.setTaskClassifier("BatchWorker");
        task.setTaskApiVersion(1);
        final BatchWorkerTask taskData = new BatchWorkerTask();
        taskData.batchType = "IngestionBatchWorkerPlugin";
        taskData.batchDefinition = batchDefinition;
        taskData.targetPipe = "worker-workflow";
        taskData.taskMessageParams = taskMessageParams;
        task.setTaskData(taskData);
        job.setTask(task);
        return job;
    }

    private int setupQueue(final String queue) throws IOException
    {
        final int prefetchCount = 1;
        channel.basicQos(prefetchCount);
        final AMQP.Queue.DeclareOk response = channel.queueDeclarePassive(queue);
        final int messageCount = response.getMessageCount();
        log.debug("Messages: " + messageCount);
        return messageCount;
    }

    private void closeQueue(final String consumerTag, final String queue) throws IOException
    {
        channel.queuePurge(queue);
        channel.basicCancel(consumerTag);
        channel.queueDelete(queue);
    }

    @SafeVarargs
    private static final Map<String, String> createTaskMessageParams(Map.Entry<String, String>... entries)
    {
        Map<String, String> testTaskMessageParams = new HashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            testTaskMessageParams.put(entry.getKey(), entry.getValue());
        }
        return testTaskMessageParams;
    }
}
