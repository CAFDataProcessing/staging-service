/*
 * Copyright 2019-2020 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.services.staging.dao.filesystem;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BatchPathProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPathProvider.class);
    private static final String INPROGRESS_FOLDER = "in_progress";
    public static final String COMPLETED_FOLDER = "completed";

    private final Path basePath;

    public BatchPathProvider(final String basePath)
    {
        this.basePath = Paths.get(basePath).normalize();
    }

    public Path getPathForTenant(final TenantId tenantId)
    {
        final Path tenantPath = basePath.resolve(tenantId.getValue()).normalize();
        if (!tenantPath.startsWith(basePath)) {
            throw new UnexpectedInvalidTenantIdException(tenantId);
        }
        return tenantPath;
    }

    public Path getPathForBatches(final TenantId tenantId)
    {
        return getPathForTenant(tenantId).resolve(COMPLETED_FOLDER);
    }

    public Path getPathForBatch(final TenantId tenantId, final BatchId batchId)
    {
        final Path pathForBatches = getPathForBatches(tenantId);
        final Path batchPath = pathForBatches.resolve(batchId.getValue()).normalize();
        if (!batchPath.startsWith(pathForBatches)) {
            throw new UnexpectedInvalidBatchIdException(batchId);
        }
        return batchPath;
    }

    public Path getInProgressPathForBatch(final TenantId tenantId, final BatchId batchId) throws StagingException
    {
        /*
        - It will create a temporary folder for the batch to store files while the entire batch is being processed
        - It will open a subbatch file with the following naming pattern "YYYYMMDD-HHMMSS-xxx-json.batch"
        (where xxx forces uniqueness - ideally it would be derived from a service instance id and the thread id).
        - It will write the metadata of each document family out into the subbatch file in minified json format.
        When a configurable subbatch size is reached the subbatch file will be closed and a new one will be opened.
        - The batch submitted by AJP may also contain loose files referenced from the document family metadata.
        Any referenced files should be submitted in the batch before they are referred to.
        The referenced files will be separately written out to a files subfolder.
        If there are any failures while processing the batch, the temporary folder will be deleted
        If the entire batch was processed without errors the temporary folder will be renamed and moved to the staging root folder
        If the batch folder existed under the staging root, it will be deleted before copying the new data
         */
        //Make a temporary folder with name like, timestamp+threadid+serviceid+batchID in a separate "in_progress" folder
        final Path tenantPath = getPathForTenant(tenantId);

        final String inProgressBatchFolderName = BatchNameProvider.getBatchDirectoryName(batchId);

        final Path inProgressPath = tenantPath.resolve(INPROGRESS_FOLDER).resolve(inProgressBatchFolderName).normalize();
        if (!inProgressPath.startsWith(tenantPath)) {
            throw new UnexpectedInvalidBatchIdException(batchId);
        }

        final File inProgressFile = inProgressPath.toFile();

        if (!inProgressFile.exists()) {
            LOGGER.debug("Creating in-progress folder for batch: {}", inProgressPath);
            final boolean dirCreated = inProgressFile.mkdirs();
            if (dirCreated) {
                LOGGER.debug("Created in-progress folder for batch: {}", inProgressPath);
            } else {
                LOGGER.error("Error creating in-progress folder for batch: {}", inProgressPath);
                throw new StagingException("In-progress folder for batch was not created : " + inProgressPath);
            }
        }
        return inProgressPath;
    }

    public Path getTenantInprogressDirectory(final TenantId tenantId)
    {
        return getPathForTenant(tenantId).resolve(INPROGRESS_FOLDER);
    }

    public static Path getStorageRefFolderPathForBatch(final TenantId tenantId, final BatchId batchId, final String storePath,
                                                       final String contentFolder)
    {
        return (new BatchPathProvider(storePath)).getPathForBatch(tenantId, batchId).resolve(contentFolder);
    }

    private static final class UnexpectedInvalidTenantIdException extends RuntimeException
    {
        public UnexpectedInvalidTenantIdException(final TenantId tenantId)
        {
            super("Invalid tenant id: " + tenantId);
        }
    }

    private static final class UnexpectedInvalidBatchIdException extends RuntimeException
    {
        public UnexpectedInvalidBatchIdException(final BatchId batchId)
        {
            super("Invalid batch id: " + batchId);
        }
    }
}
