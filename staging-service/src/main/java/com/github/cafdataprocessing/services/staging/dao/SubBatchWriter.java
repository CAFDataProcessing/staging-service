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
package com.github.cafdataprocessing.services.staging.dao;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.cafdataprocessing.services.staging.utils.JsonMinifier;

/*
 * This class handles the sub-batching and storing of documents.
 * It opens a subbatch file with the following naming pattern "yyyyMMdd-HHmmssSSS-json.batch"
 * It writes the metadata of each document family out into the subbatch file in minified json format.
 * When a configurable subbatch size is reached the subbatch file will be closed and a new one will be opened.
 */
public class SubBatchWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubBatchWriter.class);
    private static final String SUBBATCH_FILE_SUFFIX = "-json.batch";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmssSSS";

    final File inProgressBatchFolder;
    final int subbatchSize;
    private OutputStream outStream;
    //Track number of document files processed
    private int count;

    public SubBatchWriter(final File inProgressBatchFolder, final int subbatchSize)
            throws FileNotFoundException {
        this.inProgressBatchFolder = inProgressBatchFolder;
        this.subbatchSize = subbatchSize;
    }

    private void createSubBatchOutStream() throws FileNotFoundException
    {
        //Make a new subbatch file
        final String subBatchFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT))
                 + SUBBATCH_FILE_SUFFIX;
        final File subBatch = new File(inProgressBatchFolder.getAbsolutePath().concat("/").concat(subBatchFileName));
        LOGGER.debug("Created new subbatchFile : {} ", subBatch);
        //Open new stream to start writing to subbatch file
        outStream = new BufferedOutputStream(new FileOutputStream(subBatch, true));
    }

    public void writeDocumentFile(
            final InputStreamSupplier inputStreamSupplier
            ) throws JsonParseException, IOException {
        if(count == 0 || count >= subbatchSize)
        {
            //Close the stream for the current subbatch file
            closeSubBatchOutputStream();
            //reset count
            count = 0;
            createSubBatchOutStream();
        }
        final InputStream inStream = inputStreamSupplier.get();
        JsonMinifier.minifyJson(inStream, outStream);
        inStream.close();
        count++;

        LOGGER.trace("Wrote minified document to  subbatchFile");
    }

    public void closeSubBatchOutputStream() throws IOException
    {
        if(outStream != null)
        {
            LOGGER.debug("Closing subbatchFile");
            outStream.flush();
            outStream.close();
        }
    }

}
