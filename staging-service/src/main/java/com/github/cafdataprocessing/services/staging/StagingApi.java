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

import com.github.cafdataprocessing.services.staging.models.BatchList;
import com.github.cafdataprocessing.services.staging.models.StatusResponse;
import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-05-10T15:07:03.708+01:00[Europe/London]")
@Api(value = "Staging", description = "the Staging API")
public interface StagingApi {

    @ApiOperation(value = "Upload documents. The batch will be automatically created if it doesn't already exist.", nickname = "createOrReplaceBatch", notes = "", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully uploaded batch of documents."),
        @ApiResponse(code = 400, message = "The request could not be processed because one or more arguments are invalid."),
        @ApiResponse(code = 500, message = "The request failed due to an unexpected server error.") })
    @RequestMapping(value = "/batches/{batchId}",
        consumes = { "multipart/mixed" },
        method = RequestMethod.PUT)
    ResponseEntity<Void> createOrReplaceBatch(@ApiParam(value = "Identifies the tenant making the request." ,required=true) @RequestHeader(value="X-TENANT-ID", required=true) String X_TENANT_ID,@Size(min=1) @ApiParam(value = "Identifies the batch.",required=true) @PathVariable("batchId") String batchId,@ApiParam(value = ""  ) MultipartHttpServletRequest request);


    @ApiOperation(value = "Delete specified batch.", nickname = "deleteBatch", notes = "", tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Successfully deleted the batch."),
        @ApiResponse(code = 404, message = "The batch does not exist."),
        @ApiResponse(code = 500, message = "The request failed due to an unexpected server error.") })
    @RequestMapping(value = "/batches/{batchId}",
        method = RequestMethod.DELETE)
    ResponseEntity<Void> deleteBatch(@ApiParam(value = "Identifies the tenant making the request." ,required=true) @RequestHeader(value="X-TENANT-ID", required=true) String X_TENANT_ID,@Size(min=1) @ApiParam(value = "Identifies the batch.",required=true) @PathVariable("batchId") String batchId);


    @ApiOperation(value = "Retrieve the current list of batches in alphabetical order.", nickname = "getBatches", notes = "", response = BatchList.class, tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successfully retrieved batches.", response = BatchList.class),
        @ApiResponse(code = 500, message = "The request failed due to an unexpected server error.") })
    @RequestMapping(value = "/batches",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<BatchList> getBatches(@ApiParam(value = "Identifies the tenant making the request." ,required=true) @RequestHeader(value="X-TENANT-ID", required=true) String X_TENANT_ID,@Size(min=1,max=256) @ApiParam(value = "Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.") @Valid @RequestParam(value = "startsWith", required = false) String startsWith,@Size(min=1,max=256) @ApiParam(value = "Specifies the identifier to fetch batches that follow it alphabetically.") @Valid @RequestParam(value = "from", required = false) String from,@Min(1)@ApiParam(value = "Specifies the number of results to return (defaults to 25 if not specified).", allowableValues = "") @Valid @RequestParam(value = "limit", required = false) Integer limit);


    @ApiOperation(value = "Returns status information about the staging service. A client should use this to check that the service is functional before uploading a large batch.", nickname = "getStatus", notes = "", response = StatusResponse.class, tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Service is functional and can accept batches for staging.", response = StatusResponse.class),
        @ApiResponse(code = 500, message = "The request failed due to an unexpected server error.") })
    @RequestMapping(value = "/status",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<StatusResponse> getStatus(@ApiParam(value = "Identifies the tenant making the request." ,required=true) @RequestHeader(value="X-TENANT-ID", required=true) String X_TENANT_ID);

}
