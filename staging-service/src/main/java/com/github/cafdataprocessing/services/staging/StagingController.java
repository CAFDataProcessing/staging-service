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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.dao.InputStreamSupplier;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import com.github.cafdataprocessing.services.staging.models.StagedFile;
import com.github.cafdataprocessing.services.staging.models.BatchError;
import com.github.cafdataprocessing.services.staging.models.BatchList;
import com.github.cafdataprocessing.services.staging.models.BatchResponse;
import com.github.cafdataprocessing.services.staging.models.Body;
import com.github.cafdataprocessing.services.staging.models.StatusResponse;
import com.github.cafdataprocessing.services.staging.swagger.api.StagingApi;

import io.swagger.annotations.ApiParam;

@Controller
public class StagingController implements StagingApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingController.class);

    private final BatchDao fileSystemDao;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public StagingController(final BatchDao fileSystemDao, final HttpServletRequest request) {
        this.fileSystemDao = fileSystemDao;
        this.request = request;
    }

    public ResponseEntity<BatchResponse> addDocumentsToBatch(
            @Size(min=1) @ApiParam(value = "Identifies the batch.",required=true)
            @PathVariable("batchId") String batchId,
            @ApiParam(value = "") @RequestParam(value="uploadData", required=false) Body body) {
        LOGGER.debug("Staging documents for : {}", batchId);
        final BatchResponse batch = new BatchResponse();
        try
        {
            final List<String> savedFiles = saveFiles(batchId);
            //final List<String> savedFiles = saveBatchFiles(batchId);
            batch.entries(savedFiles);
            LOGGER.debug("Staged batch: {}", batch);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(batch);
        }
        catch(final FileUploadException | IOException e){
            LOGGER.error("Error getting multipart files", e);
            //TODO: Throwing runtime exception here since method signature does not allow throwing checked exception
            //TODO: Just return a 500 error with no details? Again method signature does not allow adding message in 500 error
            //because of the fixed return type
            throw new StagingException(e);
        }
    }

    public ResponseEntity<Void> deleteBatch(
            @Size(min=1)
            @ApiParam(value = "Identifies the batch.",required=true)
            @PathVariable("batchId") String batchId) {
        LOGGER.debug("Deleting batch : {}", batchId);
        try {
            fileSystemDao.deleteFiles(batchId);
            return new ResponseEntity<Void>(HttpStatus.OK);
        } catch (final IOException e) {
            LOGGER.error("Error in deleteBatch ", e);
            final BatchError err = new BatchError();
            err.setCode("IOERROR");
            err.setMessgae(e.getMessage());
            return ResponseEntity.badRequest().build();
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
            final List<String> batchFiles = fileSystemDao.getFiles(startsWith, from, limit);
            batchList.entries(batchFiles);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(batchList);
        } catch (final IOException e) {
            LOGGER.error("Error in getBatches ", e);
            final BatchError err = new BatchError();
            err.setCode("IOERROR");
            err.setMessgae(e.getMessage());
            //return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(err);
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<StatusResponse> getStatus() {
        StatusResponse status = new StatusResponse();
        status.setMessgae("Service available");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(status);
    }

    private List<String> saveFiles(final String batchId) throws FileUploadException, IOException
    {
        final ServletFileUpload fileUpload = new ServletFileUpload();
        final FileItemIterator fileItemIterator = fileUpload.getItemIterator(request);

        final Iterator<FileItemStream> iterator = new FileItemStreamIterator(fileItemIterator);
        final Iterable<FileItemStream> iterable = () -> iterator;

        final Stream<StagedFile> stagedFileStream = StreamSupport.stream(iterable.spliterator(), false)
                .map(f -> {

                    if(f.isFormField()){
                        //true if the instance represents a simple form field
                        //false if it represents an uploaded file
                        LOGGER.error("{} is not a file", f.getName());
                        throw new StagingException(f.getName() + " is not a file");
                    }
                    if (!f.getFieldName().equals("uploadData")){
                        LOGGER.error("{} is not in 'uploadData'. Invalid field name : {}", f.getName(),
                                f.getFieldName());
                        throw new StagingException(f.getName() + " is not in 'uploadData'. Invalid field name" + f.getFieldName());
                    }

                    final String contentType = f.getContentType();
                    //Need to strip any path information, according to JavaDoc for this opera will send path info
                    String filename = f.getName();
                    if (filename != null) {
                        filename = FilenameUtils.getName(filename);
                    }
                    final InputStreamSupplier inputStreamSupplier = () -> f.openStream();
                    return new StagedFile(filename, contentType, inputStreamSupplier);
        });
        return fileSystemDao.saveFiles(batchId, stagedFileStream);
    }

    //TODO: Remove: left here to see if lambda can be avoided
    private List<String> saveBatchFiles(final String batchId) throws FileUploadException, IOException
    {
        final ServletFileUpload fileUpload = new ServletFileUpload();
        final FileItemIterator fileItemIterator = fileUpload.getItemIterator(request);

        final Iterator<FileItemStream> iterator = new FileItemStreamIterator(fileItemIterator);
        final List<StagedFile> stagedFiles = new ArrayList<>();
        while(iterator.hasNext())
        {
            final FileItemStream fileItemStream = iterator.next();
            if(fileItemStream.isFormField()){
                //true if the instance represents a simple form field
                //false if it represents an uploaded file
                LOGGER.error("{} is not a file", fileItemStream.getName());
                throw new StagingException(fileItemStream.getName() + " is not a file");
            }
            if (!fileItemStream.getFieldName().equals("uploadData")){
                LOGGER.error("{} is not in 'uploadData'. Invalid field name : {}", fileItemStream.getName(),
                        fileItemStream.getFieldName());
                throw new StagingException(fileItemStream.getName() + " is not in 'uploadData'. Invalid field name"
                        + fileItemStream.getFieldName());
            }

            final String contentType = fileItemStream.getContentType();
            //Need to strip any path information, according to JavaDoc for this opera will send path info
            String filename = fileItemStream.getName();
            if (filename != null) {
                filename = FilenameUtils.getName(filename);
            }
            //TODO: throws org.apache.commons.fileupload.FileItemStream$ItemSkippedException: null
            final InputStreamSupplier inputStreamSupplier = () -> fileItemStream.openStream();
            stagedFiles.add(new StagedFile(filename, contentType, inputStreamSupplier));
        }
        return fileSystemDao.saveFiles(batchId, stagedFiles.stream());
    }

}
