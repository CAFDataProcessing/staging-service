/*
 * Copyright 2019-2021 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.services.staging.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.microfocus.caf.worker.document.schema.validator.DocumentValidator;
import com.microfocus.caf.worker.document.schema.validator.InvalidDocumentException;
import com.worldturner.medeia.api.ValidationFailedException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonMinifier
{

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMinifier.class);

    private static final String DATA_FIELD = "data";
    private static final String ENCODING_FIELD = "encoding";
    private static final String LOCAL_REF = "local_ref";
    private static final String STORAGE_REF = "storage_ref";
    private static final String UTF8_ENCODING = "utf8";
    private static final String BASE64_ENCODING = "base64";
    private static final String TXT_ENTENSION = ".txt";
    private static final String BINARY_ENTENSION = ".bin";

    private JsonMinifier()
    {
    }

    public static final void validateAndMinifyJson(final InputStream inputStream,
                                                   final OutputStream outstream,
                                                   final String storageRefPath,
                                                   final String inprogressContentFolderPath,
                                                   final int fieldValueSizeThreshold,
                                                   final Map<String, String> binaryFilesUploaded)
        throws IOException, InvalidDocumentException, InvalidBatchException
    {
        final JsonFactory factory = new JsonFactory();
        factory.configure(Feature.FLUSH_PASSED_TO_STREAM, false);
        factory.configure(Feature.AUTO_CLOSE_TARGET, false);
        final JsonParser parser = DocumentValidator.getValidatingParser(inputStream);
        try (final JsonGenerator gen = factory.createGenerator(outstream)) {
            try {
                processJsonTokens(parser, gen, storageRefPath, inprogressContentFolderPath, fieldValueSizeThreshold, binaryFilesUploaded);
            } catch (final ValidationFailedException e) {
                throw new InvalidDocumentException(e);
            }
        }
        outstream.write('\n');
    }

    public static final void minifyJson(final InputStream inputStream,
                                        final OutputStream outstream,
                                        final String storageRefPath,
                                        final String inprogressContentFolderPath,
                                        final int fieldValueSizeThreshold,
                                        final Map<String, String> binaryFilesUploaded) throws IOException, InvalidBatchException
    {
        final JsonFactory factory = new JsonFactory();
        factory.configure(Feature.FLUSH_PASSED_TO_STREAM, false);
        factory.configure(Feature.AUTO_CLOSE_TARGET, false);
        final JsonParser parser = factory.createParser(inputStream);
        try (final JsonGenerator gen = factory.createGenerator(outstream)) {
            processJsonTokens(parser, gen, storageRefPath, inprogressContentFolderPath, fieldValueSizeThreshold, binaryFilesUploaded);
        }
        outstream.write('\n');
    }

    private static void processJsonTokens(final JsonParser parser,
                                          final JsonGenerator gen,
                                          final String storageRefPath,
                                          final String inprogressContentFolderPath,
                                          final int fieldValueSizeThreshold,
                                          final Map<String, String> binaryFilesUploaded) throws IOException, InvalidBatchException
    {
        String dataBuffer = null;
        String encodingBuffer = null;
        JsonToken token;
        boolean pauseWriting = false;
        boolean bufferData = false;
        boolean bufferEncoding = false;
        boolean updateReference = false;
        // mark if the updateReference is for a file that has been uploaded
        boolean isLocalRefFile = false;
        while ((token = parser.nextToken()) != null) {
            switch (token) {
                case FIELD_NAME:
                    // Pause writing and start buffering if the 'data' or 'encoding' fields are encountered
                    if (parser.getText().equalsIgnoreCase(DATA_FIELD)) {
                        pauseWriting = true;
                        bufferData = true;
                        bufferEncoding = false;
                    } else if (parser.getText().equalsIgnoreCase(ENCODING_FIELD)) {
                        pauseWriting = true;
                        bufferEncoding = true;
                        bufferData = false;
                    } else {
                        bufferData = false;
                        bufferEncoding = false;
                        pauseWriting = false;
                    }
                    break;
                case VALUE_FALSE:
                case VALUE_NULL:
                case VALUE_NUMBER_FLOAT:
                case VALUE_NUMBER_INT:
                case VALUE_STRING:
                case VALUE_TRUE:
                    // Buffer data or encoding field values
                    if (bufferData) {
                        dataBuffer = parser.getText();
                    }
                    if (bufferEncoding) {
                        encodingBuffer = parser.getText();
                        if (encodingBuffer.equalsIgnoreCase(LOCAL_REF)) {
                            encodingBuffer = STORAGE_REF;
                            updateReference = true;
                            isLocalRefFile = true;
                        } else {
                            updateReference = false;
                        }
                    }
                    break;
                case END_OBJECT:
                    // If writing was paused, update buffered data/encoding
                    // values and write them out
                    if (pauseWriting) {
                        // check size of data field value
                        if (dataBuffer!=null && dataBuffer.getBytes().length > fieldValueSizeThreshold) {
                            // write it out to a loose file
                            final String fileName = RandomStringUtils.randomAlphanumeric(10);
                            final String contentFileName = writeDataToFile(dataBuffer, fileName, inprogressContentFolderPath, encodingBuffer);
                            dataBuffer = contentFileName;
                            encodingBuffer = STORAGE_REF;
                            updateReference = true;
                        }
                        if (updateReference) {
                            if (isLocalRefFile && !binaryFilesUploaded.keySet().contains(dataBuffer)) {
                                LOGGER.error("Binary files referenced in the JSON documents must be uploaded before the JSON documents. "
                                    + "Check file {}", dataBuffer);
                                throw new InvalidBatchException("Binary files referenced in the JSON documents must be uploaded before "
                                    + "the JSON documents. Check file " + dataBuffer);
                            } else if (isLocalRefFile && binaryFilesUploaded.keySet().contains(dataBuffer)) {
                                LOGGER.debug("The binary file has been uploaded {}, its new file name is {}",
                                             dataBuffer, binaryFilesUploaded.get(dataBuffer));
                                dataBuffer = storageRefPath + "/" + binaryFilesUploaded.get(dataBuffer);
                            } else {
                                dataBuffer = storageRefPath + "/" + dataBuffer;
                            }
                        }
                        gen.writeFieldName(DATA_FIELD);
                        gen.writeString(dataBuffer);
                        if (encodingBuffer != null) {
                            gen.writeFieldName(ENCODING_FIELD);
                            gen.writeString(encodingBuffer);
                        }
                        // clear the buffers
                        dataBuffer = null;
                        encodingBuffer = null;
                    }
                    // reset checks
                    updateReference = false;
                    pauseWriting = false;
                    isLocalRefFile = false;
                    break;
                case START_OBJECT:
                case START_ARRAY:
                case END_ARRAY:
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                default:
                    break;
            }
            if (!pauseWriting) {
                gen.copyCurrentEvent(parser);
            }
        }
    }

    private static String writeDataToFile(final String data, final String fileName,
                                          final String inprogressContentFolderPath, final String encoding) throws IOException
    {
        String contentFileName = fileName;
        if (encoding == null || encoding.equalsIgnoreCase(UTF8_ENCODING)) {
            contentFileName = fileName + TXT_ENTENSION;
            final Path targetFile = Paths.get(inprogressContentFolderPath, contentFileName);
            FileUtils.writeStringToFile(targetFile.toFile(), data, StandardCharsets.UTF_8);
        } else if (encoding.equalsIgnoreCase(BASE64_ENCODING)) {
            contentFileName = fileName + BINARY_ENTENSION;
            final Path targetFile = Paths.get(inprogressContentFolderPath, contentFileName);
            // If encoding is base64, write file after base64 decoding the data
            final byte[] decodedData = Base64.decodeBase64(data);
            FileUtils.writeByteArrayToFile(targetFile.toFile(), decodedData);
        }
        return contentFileName;
    }
}
