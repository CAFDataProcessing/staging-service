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
package com.github.cafdataprocessing.services.staging.dao.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.github.cafdataprocessing.services.staging.dao.InputStreamSupplier;
import com.github.cafdataprocessing.services.staging.exceptions.IncompleteBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafdataprocessing.services.staging.utils.JsonMinifier;
import com.microfocus.caf.worker.document.schema.validator.InvalidDocumentException;
import java.util.Map;

/*
 * This class handles the sub-batching and storing of documents.
 * It opens a subbatch file with the following naming pattern "yyyyMMdd-HHmmssSSS-json.batch"
 * It writes the metadata of each document family out into the subbatch file in minified json format.
 * When a configurable subbatch size is reached the subbatch file will be closed and a new one will be opened.
 */
public class SubBatchWriter implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubBatchWriter.class);
    private static final String SUBBATCH_FILE_SUFFIX = "-json.batch";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmssSSS";

    private final File inProgressBatchFolder;
    private final int subbatchSize;
    private OutputStream outStream;
    //Track number of document files processed
    private int count = 0;

    public SubBatchWriter(final File inProgressBatchFolder, final int subbatchSize) {
        this.inProgressBatchFolder = inProgressBatchFolder;
        this.subbatchSize = subbatchSize;
    }

    private void createSubBatchOutStream() throws StagingException
    {
        //Make a new subbatch file
        final String subBatchFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT))
                 + SUBBATCH_FILE_SUFFIX;
        final File subBatch = Paths.get(inProgressBatchFolder.toString(), subBatchFileName).toFile();
        LOGGER.debug("Created new subbatchFile : {} ", subBatch);
        //Open new stream to start writing to subbatch file

        try {
            outStream = new BufferedOutputStream(SlowOutputStream.create(new FileOutputStream(subBatch, true)));
        } catch (FileNotFoundException e) {
            throw new StagingException(e);
        }
    }

    public void writeDocumentFile(final InputStreamSupplier inputStreamSupplier,
                                  final String storageRefFolderPath,
                                  final String inprogressContentFolderPath,
                                  final int fieldValueSizeThreshold,
                                  final Map<String, String> binaryFilesUploaded)
            throws StagingException, InvalidBatchException, IncompleteBatchException {

        if(count >= subbatchSize)
        {
            //Close the stream for the current subbatch file
            try {
                close();
            } catch (Exception e) {
                throw new StagingException(e);
            }
        }

        if(outStream==null){
            createSubBatchOutStream();
        }

        try(final InputStream inStream = inputStreamSupplier.get()){
            try {
                JsonMinifier.validateAndMinifyJson(inStream, outStream, storageRefFolderPath,
                                                   inprogressContentFolderPath, fieldValueSizeThreshold, binaryFilesUploaded);
                count++;
            }
            catch (IOException | InvalidDocumentException ex){
                LOGGER.error("Error staging document", ex);
                throw new InvalidBatchException(ex);
            }
        }
        catch (IOException ex){
            throw new IncompleteBatchException(ex);
        }

        LOGGER.trace("Wrote minified document to  subbatchFile");
    }

    @Override
    public void close() throws Exception {
        if(outStream != null)
        {
            LOGGER.debug("Closing subbatchFile");
            count = 0;
            outStream.flush();
            outStream.close();
        }
        outStream = null;
    }

}
