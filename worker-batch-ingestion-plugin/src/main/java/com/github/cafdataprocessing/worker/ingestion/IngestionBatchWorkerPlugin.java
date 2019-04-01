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
import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchPathProvider;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidTenantIdException;
import com.github.cafdataprocessing.worker.ingestion.models.Subbatch;
import com.hpe.caf.worker.batch.BatchDefinitionException;
import com.hpe.caf.worker.batch.BatchWorkerPlugin;
import com.hpe.caf.worker.batch.BatchWorkerServices;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerScript;
import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Plugin that expects a batch definition as a String of IDs and will create tasks from these IDs.
 *
 * The format could be,
 *
 * tenantId/batch1|batch2|batch3|... 
 * tenantId/batch1 
 * subbatch:tenantId/batchId/file-json.batch
 *
 */
@Slf4j
public class IngestionBatchWorkerPlugin implements BatchWorkerPlugin
{
    private final ObjectMapper mapper;
    private final BatchPathProvider fileSystemProvider;

    public IngestionBatchWorkerPlugin()
    {
        final String env = System.getenv("CAF_STAGING_SERVICE_BASEPATH");
        if (StringUtils.isEmpty(env)) {
            throw new RuntimeException("CAF_STAGING_SERVICE_BASEPATH environment variable not set");
        }
        fileSystemProvider = new BatchPathProvider(env);
        mapper = new ObjectMapper();
    }

    @Override
    public void processBatch(final BatchWorkerServices batchWorkerServices, final String batchDefinition,
                             final String taskMessageType, final Map<String, String> taskMessageParams)
        throws BatchDefinitionException
    {
        log.debug("Definitions received: " + batchDefinition);
        if (taskMessageParams != null) {
            log.debug("Task message params: " + taskMessageParams.toString());
        }

        if (batchDefinition == null || batchDefinition.trim().length() == 0) {
            log.error("IngestionBatchWorkerPlugin has not received a valid batch definition string");
            throw new RuntimeException("IngestionBatchWorkerPlugin has not received a valid batch definition string");
        }
        if (batchDefinition.startsWith("subbatch:")) {
            try {
                // this is already a subbatch
                // read the file from the staging and send it through
                log.debug("Subbatch received: " + batchDefinition);
                handleSubbatch(batchDefinition, batchWorkerServices, taskMessageParams);
            } catch (InvalidBatchIdException ex) {
                log.error("Invalid Batch Id Exception! " + ex.getMessage());
                throw new RuntimeException("Invalid Batch Id Exception! " + ex.getMessage());
            }
        } else if (batchDefinition.contains("|")) {
            log.debug("Multiple Batches received: " + batchDefinition);
            handleMultipleBatchIds(batchDefinition, batchWorkerServices);
        } else {
            log.debug("Single Batch received: " + batchDefinition);
            handleSingleBatchId(batchDefinition, batchWorkerServices);
        }

    }

    private void handleMultipleBatchIds(final String batchIds, final BatchWorkerServices batchWorkerServices)
    {
        final String tenantId = extractTenantId(batchIds);
        final String[] batchesSplit = extractBatchIds(batchIds);
        for (final String batch : batchesSplit) {
            batchWorkerServices.registerBatchSubtask(tenantId + "/" + batch);
        }
    }

    private void handleSingleBatchId(final String batchId, final BatchWorkerServices batchWorkerServices)
    {
        try {
            final TenantId tenantId = new TenantId(extractTenantId(batchId));
            final BatchId batchIdExtracted = new BatchId(extractBatchIds(batchId)[0]);
            final Path pathOfSubBatches = fileSystemProvider.getPathForBatch(tenantId, batchIdExtracted);
            final String[] extensions = {"batch"};
            final Collection<File> subbatchesFiles = FileUtils.listFiles(pathOfSubBatches.toFile(), extensions, false);

            for (final File subbatch : subbatchesFiles) {
                log.debug("Batch file found: " + FilenameUtils.getName(subbatch.getAbsolutePath()));
                batchWorkerServices.registerBatchSubtask("subbatch:" + tenantId.getValue() + "/" + batchIdExtracted.getValue()
                    + "/" + FilenameUtils.getName(subbatch.getAbsolutePath()));
            }
        } catch (InvalidBatchIdException | InvalidTenantIdException ex) {
            log.error("Exception while handling single batch id: " + ex.getMessage());
            throw new RuntimeException("Exception while handling a single batch id: " + ex.getMessage());
        }

    }

    private void handleSubbatch(final String subbatch, final BatchWorkerServices batchWorkerServices,
                                final Map<String, String> taskMessageParams) throws InvalidBatchIdException
    {
        final Subbatch subbatchObj = extractSubbatch(subbatch);
        final Path pathOfSubBatches = fileSystemProvider.getPathForBatch(subbatchObj.getTenantId(), subbatchObj.getBatchId());
        final Path batchFileName = Paths.get(pathOfSubBatches.toString(), subbatchObj.getFileName());
        log.debug("I am going to read each line of: " + batchFileName);
        try (Stream<String> lines = Files.lines(batchFileName)) {
            lines.forEach(line -> {
                try {
                    final DocumentWorkerDocumentTask document = mapper.readValue(line, DocumentWorkerDocumentTask.class);
                    final Map<String, String> customData = populateCustomData(taskMessageParams);
                    if (!customData.isEmpty()) {
                        document.customData = customData;
                    }
                    final List<DocumentWorkerScript> scripts = populateScripts(taskMessageParams);
                    if (!scripts.isEmpty()) {
                        document.scripts = scripts;
                    }
                    log.debug("I am going to registers an Item Subtask for: " + document.document.reference);
                    batchWorkerServices.registerItemSubtask(DocumentWorkerConstants.DOCUMENT_TASK_NAME,
                                                            DocumentWorkerConstants.WORKER_API_VER, document);
                } catch (IOException ex) {
                    log.error("Exception while deserializing the json of " + line + "\nFile: " + batchFileName + "\n"
                        + ex.getMessage());
                    throw new RuntimeException("Exception while deserializing the json of " + line + "\nFile: " + batchFileName + "\n"
                        + ex.getMessage());
                } catch (BatchDefinitionException ex) {
                    log.error("BatchDefinitionException " + ex.getMessage());
                    throw new RuntimeException("BatchDefinitionException " + ex.getMessage());
                }
            });
        } catch (IOException ex) {
            log.error("Exception while trying to read lines from: " + batchFileName + "\n" + ex.getMessage());
            throw new RuntimeException("Exception while trying to read lines from: " + batchFileName + "\n" + ex.getMessage());
        }
    }

    private String extractTenantId(final String batchIds)
    {
        final int tenantDelimiter = batchIds.indexOf("/");
        if (tenantDelimiter == -1) {
            log.error("The tenant id is not present in the string passed");
            throw new RuntimeException("The tenant id is not present in the string passed");
        }
        final String tenantId = batchIds.substring(0, tenantDelimiter);
        if (StringUtils.isEmpty(tenantId)) {
            log.error("The tenant id is not present in the string passed");
            throw new RuntimeException("The tenant id is not present in the string passed");
        }
        return tenantId;
    }

    private String[] extractBatchIds(final String batchIds)
    {
        final int tenantDelimiter = batchIds.indexOf("/");
        final String[] batchesSplit = batchIds.substring(tenantDelimiter + 1, batchIds.length()).split("\\|");
        if (batchesSplit.length == 0) {
            log.error("No batch ids were found");
            throw new RuntimeException("No batch ids were found");
        }
        log.debug("Batch Id(s) found: " + Arrays.toString(batchesSplit));
        return batchesSplit;
    }

    private Subbatch extractSubbatch(final String subbatch)
    {
        final String subbatchOnlyPart = subbatch.substring(subbatch.lastIndexOf("/") + 1, subbatch.length());
        if (StringUtils.isEmpty(subbatchOnlyPart)) {
            log.error("No subbatch was found");
            throw new RuntimeException("No subbatch was found");
        }
        final String tenantId = extractTenantId(subbatch.substring(subbatch.indexOf(":") + 1, subbatch.length()));
        final String batchId = extractBatchIds(subbatch.substring(subbatch.indexOf(":") + 1, subbatch.lastIndexOf("/")))[0];
        try {
            return new Subbatch(subbatchOnlyPart, tenantId, batchId);
        } catch (InvalidBatchIdException | InvalidTenantIdException ex) {
            log.error("Exception: " + ex.getMessage());
            throw new RuntimeException("Exception: " + ex.getMessage());
        }
    }

    private Map<String, String> populateCustomData(final Map<String, String> taskMessageParams) throws BatchDefinitionException
    {
        final Map<String, String> map = new HashMap<>();
        if (taskMessageParams != null && !taskMessageParams.isEmpty()) {
            for (final Map.Entry<String, String> taskParam : taskMessageParams.entrySet()) {
                if (!taskParam.getKey().contains(":")) {
                    log.error("Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or script. Key: " + taskParam.getKey() + ", Value: " + taskParam.getValue());
                    throw new BatchDefinitionException(String.format(
                        "Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or script.", taskParam.getKey(), taskParam.getValue()));
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

    private List<DocumentWorkerScript> populateScripts(final Map<String, String> taskMessageParams) throws BatchDefinitionException
    {
        final List<DocumentWorkerScript> list = new ArrayList<>();
        if (taskMessageParams != null && !taskMessageParams.isEmpty()) {
            for (final Map.Entry<String, String> taskParam : taskMessageParams.entrySet()) {
                if (!taskParam.getKey().contains(":")) {
                    log.error("Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or script. Key: " + taskParam.getKey() + ", Value: " + taskParam.getValue());
                    throw new BatchDefinitionException(String.format(
                        "Unable to act on task param as it contains an unrecognized field. Task param key was %s,"
                        + " task param value was %s. Field names should be prefixed with one of the following, field, cd, "
                        + "customdata or script.", taskParam.getKey(), taskParam.getValue()));
                }
                final String paramType = taskParam.getKey().substring(0, taskParam.getKey().indexOf(":"));
                if (paramType.equals("scripts")) {
                    final DocumentWorkerScript script = new DocumentWorkerScript();
                    script.name = createKey(taskParam.getKey());
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

    private String createKey(final String customDataMapKey)
    {
        return customDataMapKey.substring(customDataMapKey.indexOf(":") + 1, customDataMapKey.length());
    }

}
