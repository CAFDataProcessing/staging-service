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
package com.github.cafdataprocessing.services.staging;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.github.cafdataprocessing.services.staging.exceptions.*;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.models.BatchList;
import com.github.cafdataprocessing.services.staging.models.StatusResponse;
import com.github.cafdataprocessing.services.staging.swagger.api.StagingApi;

import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StagingController implements StagingApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingController.class);

    private final BatchDao batchDao;

    private final HttpServletRequest request;

    @Autowired
    public StagingController(final BatchDao fileSystemDao, final HttpServletRequest request) {
        this.batchDao = fileSystemDao;
        this.request = request;
    }

    public ResponseEntity<Void> createOrReplaceBatch(
            @Size(min=1) @ApiParam(value = "Identifies the batch.",required=true)
            @PathVariable("batchId") String batchId,
            Object body) {

        final ServletFileUpload fileUpload = new ServletFileUpload();
        final FileItemIterator fileItemIterator;
        try{
            fileItemIterator = fileUpload.getItemIterator(request);
        }
        catch(final FileUploadException | IOException ex){
            LOGGER.error("Error getting FileItemIterator", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        try
        {
            final List<String> savedFiles = batchDao.saveFiles(new BatchId(batchId), fileItemIterator);
            LOGGER.debug("Staged batch: {}, entries: {}", batchId, savedFiles);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch(final InvalidBatchIdException | IncompleteBatchException | InvalidBatchException ex){
            LOGGER.error("Error getting multipart files", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());

        }
        catch(final StagingException ex){
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<Void> deleteBatch(
            @Size(min=1)
            @ApiParam(value = "Identifies the batch.",required=true)
            @PathVariable("batchId") String batchId) {
        LOGGER.debug("Deleting batch : {}", batchId);
        try {
            batchDao.deleteBatch(new BatchId(batchId));
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (final InvalidBatchIdException ex){
            LOGGER.error("Invalid batchId ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        catch (final BatchNotFoundException ex) {
            LOGGER.error("Error in deleteBatch ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.NOT_FOUND, ex.getMessage());

        }
        catch(final StagingException ex){
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<BatchList> getBatches(
            @Size(min=1,max=256)
            @ApiParam(value = "Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.")
            @Valid @RequestParam(value = "startsWith", required = false) String startsWith,
            @Size(min=1,max=256) @ApiParam(value = "Specifies the identifier to fetch batches that follow it alphabetically.")
            @Valid @RequestParam(value = "from", required = false) String from,
            @Min(1) @ApiParam(value = "Specifies the number of results to return (defaults to 25 if not specified).", allowableValues = "")
            @Valid @RequestParam(value = "limit", required = false) Integer limit) {

        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        final BatchList batchList = new BatchList();
        try {
            final BatchId fromBatchId;
            if(from == null)
            {
                fromBatchId = null;
            }
            else
            {
                fromBatchId = new BatchId(from);
            }
            final List<String> batchFiles = batchDao.getBatches(startsWith, fromBatchId, limit);
            batchList.entries(batchFiles);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(batchList);
        }
        catch (final InvalidBatchIdException ex) {
            LOGGER.error("Invalid batchId ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        catch (final StagingException ex) {
            LOGGER.error("Error in getBatches ", ex);
            throw new WebMvcHandledRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<StatusResponse> getStatus() {
        StatusResponse status = new StatusResponse();
        status.setMessage("Service available");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(status);
    }

}
