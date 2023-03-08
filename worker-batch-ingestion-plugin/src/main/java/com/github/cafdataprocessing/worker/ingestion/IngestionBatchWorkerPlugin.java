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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchPathProvider;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidTenantIdException;
import com.github.cafdataprocessing.worker.ingestion.models.Subbatch;
import com.github.cafdataprocessing.worker.ingestion.validator.FieldValidator;
import com.github.cafdataprocessing.worker.ingestion.validator.ValidationFileAdapterException;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerPlugin;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.batch.BatchWorkerTransientException;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerScript;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Plugin that expects a batch definition as a String of IDs and will create tasks from these IDs.
 *
 * The format could be:
 * <ul>
 * <li>tenantId/batch1|batch2|batch3|...</li>
 * <li>tenantId/batch1</li>
 * <li>subbatch:tenantId/batchId/file-json.batch</li>
 * </ul>
 */
@Slf4j
public final class IngestionBatchWorkerPlugin implements BatchWorkerPlugin
{
    private final ObjectMapper mapper;
    private final BatchPathProvider fileSystemProvider;
    private final String validationFile;
    private final FieldValidator fieldValidator;

    public IngestionBatchWorkerPlugin()
    {
        final String env = System.getenv("CAF_STAGING_SERVICE_BASEPATH");
        if (StringUtils.isEmpty(env)) {
            throw new RuntimeException("CAF_STAGING_SERVICE_BASEPATH environment variable not set");
        }

        final Optional<Integer> totalSubdocumentLimit = Stream.of(
            System.getenv("CAF_INGESTION_BATCH_WORKER_SUBDOCUMENT_LIMIT"),
            "1000"
        ).filter(StringUtils::isNotBlank).map(Integer::parseInt).findFirst();

        if (!totalSubdocumentLimit.isPresent()) {
            throw new RuntimeException("CAF_INGESTION_BATCH_WORKER_SUBDOCUMENT_LIMIT was not supplied and the default logic failed.");
        }

        fileSystemProvider = new BatchPathProvider(env);
        mapper = new ObjectMapper();

        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(DocumentWorkerDocument.class,
                                     new DocumentWorkerDocumentDeserializer(totalSubdocumentLimit.get()));
        mapper.registerModule(simpleModule);

        validationFile = System.getenv("CAF_VALIDATION_FILE");
        if (StringUtils.isNotEmpty(validationFile)) {
            try {
                fieldValidator = new FieldValidator(validationFile);
            } catch (final ValidationFileAdapterException validationFileAdapterException) {
                log.error("Exception when attempting to read validation file" + "\n"
                    + validationFileAdapterException.getMessage());
                throw new RuntimeException("Exception when attempting to read validation file" + "\n"
                    + validationFileAdapterException.getMessage());
            }
        } else {
            fieldValidator = new FieldValidator();
        }
    }

    @Override
    public void processBatch(final BatchWorkerServices batchWorkerServices, final String batchDefinition,
                             final String taskMessageType, final Map<String, String> taskMessageParams)
        throws BatchDefinitionException, BatchWorkerTransientException
    {
        log.debug("Definitions received: " + batchDefinition);
        if (taskMessageParams != null) {
            log.debug("Task message params: " + taskMessageParams.toString());
        }

        if (batchDefinition == null || batchDefinition.trim().length() == 0) {
            log.error("IngestionBatchWorkerPlugin has not received a valid batch definition string");
            throw new BatchDefinitionException("IngestionBatchWorkerPlugin has not received a valid batch definition string");
        }
        if (batchDefinition.contains(":")) {
            if (batchDefinition.startsWith("subbatch:")) {
                try {
                    // this is already a subbatch
                    // read the file from the staging and send it through
                    log.debug("Subbatch received: " + batchDefinition);
                    handleSubbatch(batchDefinition, batchWorkerServices, taskMessageParams);
                } catch (final InvalidBatchIdException ex) {
                    log.error("Invalid Batch Id Exception! " + ex.getMessage());
                    throw new BatchDefinitionException("Invalid Batch Id Exception! " + ex.getMessage());
                }
            } else {
                log.error("Invalid format of the batch definition: " + batchDefinition);
                throw new BatchDefinitionException("Invalid format of the batch definition: " + batchDefinition);
            }
        } else if (batchDefinition.contains("|")) {
            log.debug("Multiple Batches received: " + batchDefinition);
            try {
                handleMultipleBatchIds(batchDefinition, batchWorkerServices);
            } catch (final InvalidTenantIdException | InvalidBatchIdException ex) {
                log.error("Invalid Batch or Tenant Id Exception! " + ex.getMessage());
                throw new BatchDefinitionException("Invalid Tenant or Batch Id Exception! " + ex.getMessage());
            }
        } else {
            log.debug("Single Batch received: " + batchDefinition);
            handleSingleBatchId(batchDefinition, batchWorkerServices);
        }
    }

    private static void handleMultipleBatchIds(final String batchIds, final BatchWorkerServices batchWorkerServices)
        throws BatchDefinitionException, InvalidTenantIdException, InvalidBatchIdException
    {
        final TenantId tenantId = new TenantId(extractTenantId(batchIds));
        final List<BatchId> batchesSplit = extractBatchIds(batchIds);
        for (final BatchId batch : batchesSplit) {
            batchWorkerServices.registerBatchSubtask(tenantId.getValue() + "/" + batch.getValue());
        }
    }

    private void handleSingleBatchId(final String batchId, final BatchWorkerServices batchWorkerServices) throws BatchDefinitionException
    {
        try {
            final TenantId tenantId = new TenantId(extractTenantId(batchId));
            final BatchId batchIdExtracted = extractBatchIds(batchId).get(0);
            final Path pathOfSubBatches = fileSystemProvider.getPathForBatch(tenantId, batchIdExtracted);

            throwIOExceptionIfFileIsNotAccessible(pathOfSubBatches);
            final String[] extensions = {"batch"};
            final Collection<File> subbatchesFiles = FileUtils.listFiles(pathOfSubBatches.toFile(), extensions, false);

            for (final File subbatch : subbatchesFiles) {
                log.debug("Batch file found: " + subbatch.getName());
                batchWorkerServices.registerBatchSubtask("subbatch:" + tenantId.getValue() + "/" + batchIdExtracted.getValue()
                    + "/" + subbatch.getName());
            }
        } catch (final IllegalArgumentException ex) {
            log.error("Exception while reading the batch: " + batchId
                + " was not found");
            throw new BatchDefinitionException("Exception while reading the batch: " + batchId
                + " was not found");
        } catch (final InvalidBatchIdException | InvalidTenantIdException ex) {
            log.error("Exception while handling single batch id: " + ex.getMessage());
            throw new BatchDefinitionException("Exception while handling a single batch id: " + ex.getMessage());
        } catch (final IOException ex) {
            log.error("Exception while reading the batch: " + batchId + " was not found", ex);
            throw new BatchDefinitionException("Exception while reading the batch: " + batchId + " was not found", ex);
        }
    }

    private static void throwIOExceptionIfFileIsNotAccessible(final Path path) throws IOException
    {
        path.getFileSystem().provider().checkAccess(path);
    }

    private void handleSubbatch(final String subbatch, final BatchWorkerServices batchWorkerServices,
                                final Map<String, String> taskMessageParams)
        throws InvalidBatchIdException, BatchWorkerTransientException, BatchDefinitionException
    {
        final Subbatch subbatchObj = extractSubbatch(subbatch);
        final Path pathOfSubBatches = fileSystemProvider.getPathForBatch(subbatchObj.getTenantId(), subbatchObj.getBatchId());
        final Path subbatchFileName = Paths.get(pathOfSubBatches.toString(), subbatchObj.getFileName());
        log.debug("I am going to read each line of: " + subbatchFileName);
        final List<String> lines = new ArrayList<>();
        try {
            throwBatchExceptionIfFileIsNotAccessible(subbatch, subbatchFileName);
            lines.addAll(Files.readAllLines(subbatchFileName));
        } catch (final IOException ex) {
            log.error("Transient exception while reading subbatch: " + subbatchFileName + ", message: " + ex.getMessage());
            throw new BatchWorkerTransientException("Transient exception while reading subbatch: " + subbatchFileName + ", message: "
                + ex.getMessage());
        }
        final List<DocumentWorkerDocumentTask> documents = new ArrayList<>();
        for (final String line : lines) {
            try {
                documents.add(createDocument(line, taskMessageParams));
            } catch (final IOException ex) {
                // exception from jackson readValue()
                log.error("Exception while deserializing the json of " + line + "\nFile: " + subbatchFileName + "\n" + ex.getMessage());
                throw new RuntimeException("Exception while deserializing the json of " + line + "\nFile: " + subbatchFileName + "\n"
                    + ex.getMessage());
            }
        }
        for (final DocumentWorkerDocumentTask document : documents) {
            log.debug("I am going to registers an Item Subtask for: " + document.document.reference);
            batchWorkerServices.registerItemSubtask(DocumentWorkerConstants.DOCUMENT_TASK_NAME, 1, document);
        }
    }

    private static void throwBatchExceptionIfFileIsNotAccessible(final String subbatch, final Path subbatchFileName)
        throws BatchDefinitionException
    {
        try {
            throwIOExceptionIfFileIsNotAccessible(subbatchFileName);
        } catch (final IOException ex) {
            log.error("Exception while reading subbatch: " + subbatch + ", it does not exist", ex);
            throw new BatchDefinitionException("Exception while reading subbatch: " + subbatch + ", it does not exist", ex);
        }
    }

    private DocumentWorkerDocumentTask createDocument(final String line, final Map<String, String> taskMessageParams)
        throws IOException, BatchDefinitionException
    {
        final DocumentWorkerDocumentTask document = mapper.readValue(line, DocumentWorkerDocumentTask.class);
        document.document.failures = new ArrayList<>();

        if (StringUtils.isNotEmpty(validationFile)) {
            document.document = fieldValidator.validate(document.document);
        }

        final Map<String, String> customData = populateCustomData(taskMessageParams);
        if (!customData.isEmpty()) {
            document.customData = customData;
        }
        final List<DocumentWorkerScript> scripts = populateScripts(taskMessageParams);
        if (!scripts.isEmpty()) {
            document.scripts = scripts;
        }
        return document;
    }

    private static String extractTenantId(final String batchIds) throws BatchDefinitionException
    {
        final int tenantDelimiter = batchIds.indexOf("/");
        if (tenantDelimiter == -1) {
            log.error("The tenant id is not present in the string passed");
            throw new BatchDefinitionException("The tenant id is not present in the string passed");
        }
        final String tenantId = batchIds.substring(0, tenantDelimiter);
        if (tenantId.isEmpty()) {
            log.error("The tenant id is not present in the string passed");
            throw new BatchDefinitionException("The tenant id is not present in the string passed");
        }
        return tenantId;
    }

    private static List<BatchId> extractBatchIds(final String batchIds) throws BatchDefinitionException, InvalidBatchIdException
    {
        final List<BatchId> batchList = new ArrayList<>();
        final int tenantDelimiter = batchIds.indexOf("/");
        final String[] batchesSplit = batchIds.substring(tenantDelimiter + 1, batchIds.length()).split("\\|");
        if (batchesSplit.length == 0) {
            log.error("No batch ids were found");
            throw new BatchDefinitionException("No batch ids were found");
        }
        log.debug("Batch Id(s) found: " + Arrays.toString(batchesSplit));
        for (final String batch : batchesSplit) {
            batchList.add(new BatchId(batch));
        }
        return batchList;
    }

    private static Subbatch extractSubbatch(final String subbatch) throws BatchDefinitionException, InvalidBatchIdException
    {
        final String subbatchOnlyPart = subbatch.substring(subbatch.lastIndexOf("/") + 1, subbatch.length());
        if (StringUtils.isEmpty(subbatchOnlyPart)) {
            log.error("No subbatch was found");
            throw new BatchDefinitionException("No subbatch was found");
        }
        final String tenantId = extractTenantId(subbatch.substring(subbatch.indexOf(":") + 1, subbatch.length()));
        final String batchId = extractBatchIds(subbatch.substring(subbatch.indexOf(":") + 1, subbatch.lastIndexOf("/")))
            .get(0).getValue();
        try {
            return new Subbatch(subbatchOnlyPart, tenantId, batchId);
        } catch (final InvalidBatchIdException | InvalidTenantIdException ex) {
            log.error("Exception: " + ex.getMessage());
            throw new BatchDefinitionException("Exception: " + ex.getMessage());
        }
    }

    private static Map<String, String> populateCustomData(final Map<String, String> taskMessageParams) throws BatchDefinitionException
    {
        final Map<String, String> map = new HashMap<>();
        if (taskMessageParams != null && !taskMessageParams.isEmpty()) {
            for (final Map.Entry<String, String> taskParam : taskMessageParams.entrySet()) {
                if (!taskParam.getKey().contains(":")) {
                    log.error("Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or graaljs. Key: " + taskParam.getKey() + ", Value: " + taskParam.getValue());
                    throw new BatchDefinitionException(String.format(
                        "Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or graaljs.", taskParam.getKey(), taskParam.getValue()));
                }
                final String paramType = taskParam.getKey().substring(0, taskParam.getKey().indexOf(":"));
                if (paramType.equals("customdata")) {
                    map.put(createKey(taskParam.getKey()), taskParam.getValue());
                } else if (paramType.equals("cd")) {
                    map.put(createKey(taskParam.getKey()), taskParam.getValue());
                }
            }
        }
        for (final Entry<String, String> entry : map.entrySet()) {
            log.debug("Custom Data found: " + entry.getKey() + ", value: " + entry.getValue());
        }
        return map;
    }

    private static List<DocumentWorkerScript> populateScripts(final Map<String, String> taskMessageParams) throws BatchDefinitionException
    {
        final List<DocumentWorkerScript> list = new ArrayList<>();
        if (taskMessageParams != null && !taskMessageParams.isEmpty()) {
            for (final Map.Entry<String, String> taskParam : taskMessageParams.entrySet()) {
                if (!taskParam.getKey().contains(":")) {
                    log.error("Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or graaljs. Key: " + taskParam.getKey() + ", Value: " + taskParam.getValue());
                    throw new BatchDefinitionException(String.format(
                        "Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or graaljs.", taskParam.getKey(), taskParam.getValue()));
                }
                final String paramType = taskParam.getKey().substring(0, taskParam.getKey().indexOf(":"));
                if (paramType.equals("scripts")) {
                    final DocumentWorkerScript script = new DocumentWorkerScript();
                    script.name = createKey(taskParam.getKey());
                    script.script = taskParam.getValue();
                    list.add(script);
                } else if (paramType.equals("graaljs")) {
                    final DocumentWorkerScript script = new DocumentWorkerScript();
                    script.name = createKey(taskParam.getKey());
                    script.engine = "GRAAL_JS";
                    script.script = taskParam.getValue();
                    list.add(script);
                }
            }
        }
        for (final DocumentWorkerScript script : list) {
            log.debug("Script found: " + script.name);
        }
        return list;
    }

    /**
     * This an utility method that helps to extract the key value of a custom map or the name of the script of the task message
     * parameters.
     *
     * They arrive in a format similar to, customData:key or scripts:name, and it returns only the "key" or "name" part.
     *
     * @param key the string to parse
     * @return only the valid key for custom data or the name of the script
     */
    private static String createKey(final String key)
    {
        return key.substring(key.indexOf(":") + 1, key.length());
    }
}
