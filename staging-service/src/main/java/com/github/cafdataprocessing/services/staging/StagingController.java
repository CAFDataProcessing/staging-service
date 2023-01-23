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
package com.github.cafdataprocessing.services.staging;

import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.exceptions.*;
import com.github.cafdataprocessing.services.staging.models.BatchList;
import com.github.cafdataprocessing.services.staging.models.BatchStatusResponse;
import com.github.cafdataprocessing.services.staging.models.StatusResponse;
import com.github.cafdataprocessing.services.staging.swagger.api.StagingApi;
import com.github.cafdataprocessing.services.staging.utils.BatchProgressTracker;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@DependsOn({"basePathCreator"})
public class StagingController implements StagingApi
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingController.class);

    private final BatchDao batchDao;

    private final HttpServletRequest request;

    private final DiskSpaceHealthIndicatorWithTimeout diskSpaceHealthIndicatorWithTimeout;

    private final DiskAccessHealthIndicatorWithTimeout diskAccessHealthIndicatorWithTimeout;

    @Autowired
    public StagingController(
        final BatchDao fileSystemDao,
        final HttpServletRequest request,
        final DiskSpaceHealthIndicatorProperties diskSpaceHealthIndicatorProperties,
        final StagingProperties stagingProperties)
    {
        this.batchDao = fileSystemDao;
        this.request = request;
        this.diskSpaceHealthIndicatorWithTimeout = new DiskSpaceHealthIndicatorWithTimeout(
            diskSpaceHealthIndicatorProperties.getPath(),
            diskSpaceHealthIndicatorProperties.getThreshold(),
            stagingProperties.getHealthcheckTimeoutSeconds());
        this.diskAccessHealthIndicatorWithTimeout = new DiskAccessHealthIndicatorWithTimeout(
            diskSpaceHealthIndicatorProperties.getPath().toPath(),
            stagingProperties.getHealthcheckTimeoutSeconds());
    }

    public ResponseEntity<Void> createOrReplaceBatch(
        @ApiParam(value = "Identifies the tenant making the request.", required = true)
        @RequestHeader(value = "X-TENANT-ID", required = true) String X_TENANT_ID,
        @Size(min = 1) @ApiParam(value = "Identifies the batch.", required = true)
        @PathVariable("batchId") String batchId,
        HttpServletRequest request)
    {
        final ServletFileUpload fileUpload = new ServletFileUpload();
        final BatchProgressTracker batchProgressTracker = new BatchProgressTracker();
        batchProgressTracker.updateTracker(fileUpload);
        final FileItemIterator fileItemIterator;
        try {
            fileItemIterator = fileUpload.getItemIterator(request);
        } catch (final FileUploadException | IOException ex) {
            LOGGER.error("Error getting FileItemIterator", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        try {
            batchDao.saveFiles(new TenantId(X_TENANT_ID), new BatchId(batchId), fileItemIterator);
            LOGGER.debug("Staged batch: {}", batchId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (final InvalidTenantIdException | InvalidBatchIdException | IncompleteBatchException | InvalidBatchException ex) {
            LOGGER.error("Error getting multipart files", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final StagingException ex) {
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        } finally {
            batchProgressTracker.remove(Thread.currentThread().getId(), ServiceIdentifier.getServiceId());
        }
    }

    public ResponseEntity<Void> deleteBatch(
        @ApiParam(value = "Identifies the tenant making the request.", required = true)
        @RequestHeader(value = "X-TENANT-ID", required = true) String X_TENANT_ID,
        @Size(min = 1)
        @ApiParam(value = "Identifies the batch.", required = true)
        @PathVariable("batchId") String batchId)
    {
        LOGGER.debug("Deleting batch : {}", batchId);
        try {
            batchDao.deleteBatch(new TenantId(X_TENANT_ID), new BatchId(batchId));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (final InvalidTenantIdException ex) {
            LOGGER.error("Invalid X-TENANT-ID ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final InvalidBatchIdException ex) {
            LOGGER.error("Invalid batchId ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final BatchNotFoundException ex) {
            LOGGER.error("Error in deleteBatch ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.NOT_FOUND, ex.getMessage());

        } catch (final StagingException ex) {
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<BatchStatusResponse> getBatchStatus(
            @ApiParam(value = "Identifies the tenant making the request.", required = true)
            @RequestHeader(value = "X-TENANT-ID", required = true) String X_TENANT_ID,
            @ApiParam(value = "Identifies the batch.", required = true)
            @RequestParam("batchId") String batchId)
    {
        try {
            final BatchStatusResponse statusResponse = batchDao.getBatchStatus(new TenantId(X_TENANT_ID), new BatchId(batchId));
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(statusResponse);
        } catch (final InvalidTenantIdException ex) {
            LOGGER.error("Invalid X-TENANT-ID.", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final InvalidBatchIdException ex) {
            LOGGER.error("Invalid Batch Id.", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final BatchNotFoundException ex) {
            LOGGER.error("Batch not found.", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (final StagingException ex) {
            LOGGER.error("Internal server error.", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        } catch (ServiceUnavailableException ex) {
            LOGGER.error("Service Unavailable.", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }
    }

    public ResponseEntity<BatchList> getBatches(
        @ApiParam(value = "Identifies the tenant making the request.", required = true)
        @RequestHeader(value = "X-TENANT-ID", required = true) String X_TENANT_ID,
        @Size(min = 1, max = 256)
        @ApiParam(value = "Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.")
        @Valid @RequestParam(value = "startsWith", required = false) String startsWith,
        @Size(min = 1, max = 256) @ApiParam(value = "Specifies the identifier to fetch batches that follow it alphabetically.")
        @Valid @RequestParam(value = "from", required = false) String from,
        @Min(1) @ApiParam(value = "Specifies the number of results to return (defaults to 25 if not specified).", allowableValues = "")
        @Valid @RequestParam(value = "limit", required = false) Integer limit)
    {

        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        final BatchList batchList = new BatchList();
        try {
            final BatchId fromBatchId;
            if (from == null) {
                fromBatchId = null;
            } else {
                fromBatchId = new BatchId(from);
            }
            final List<String> batchFiles = batchDao.getBatches(new TenantId(X_TENANT_ID), startsWith, fromBatchId, limit);
            batchList.entries(batchFiles);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(batchList);
        } catch (final InvalidTenantIdException ex) {
            LOGGER.error("Invalid X-TENANT-ID ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final InvalidBatchIdException ex) {
            LOGGER.error("Invalid batchId ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (final StagingException ex) {
            LOGGER.error("Error in getBatches ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<StatusResponse> getStatus(
        @ApiParam(value = "Identifies the tenant making the request.", required = true)
        @RequestHeader(value = "X-TENANT-ID", required = true) String X_TENANT_ID
    )
    {
        final StatusResponse status = new StatusResponse();

        final Health diskSpaceHealth = diskSpaceHealthIndicatorWithTimeout.health();

        final Health diskAccessHealth = diskAccessHealthIndicatorWithTimeout.health();

        if ((diskSpaceHealth.getStatus() == Status.UP) && (diskAccessHealth.getStatus() == Status.UP)) {
            status.setMessage("Service available");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(status);
        } else {
            status.setMessage(String.format("Service unavailable due to [access details Status:%s , %s disk space details Status:%s, %s]",
                                            diskAccessHealth.getStatus(), diskAccessHealth.getDetails(),
                                            diskSpaceHealth.getStatus(),
                                            diskSpaceHealth.getDetails()));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(status);
        }
    }
}
