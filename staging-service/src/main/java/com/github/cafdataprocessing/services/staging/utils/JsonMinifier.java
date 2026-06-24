/*
 * Copyright 2019-2026 Open Text.
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.github.cafdataprocessing.workers.document.schema.validator.DocumentValidator;
import com.github.cafdataprocessing.workers.document.schema.validator.InvalidDocumentException;
import com.worldturner.medeia.api.ValidationFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.StringEscapeUtils;
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
    private static final String RAW_UTF8_BYTES_SUFFIX = "_STAGED_RAW_UTF8_BYTES";
    private static final String JSON_ESCAPED_UTF8_BYTES_SUFFIX = "_STAGED_JSON_ESCAPED_UTF8_BYTES";

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
        // When a large text value is moved out to a staged NFS file, the JSON field value is replaced
        // with a storage_ref:
        //
        //   "CONTENT": [{"data": "very large text value..."}]
        //
        // becomes:
        //
        //   "CONTENT": [{"data": "/staged/files/abc123.txt", "encoding": "storage_ref"}]
        //
        // Downstream components may expand that storage_ref back into a JSON-escaped string. To let
        // those components make size-related decisions without opening and escaping the staged file
        // again, staging records sibling metadata fields beside the original field:
        //
        //   "CONTENT_STAGED_RAW_UTF8_BYTES": [{"data": "125829120"}],
        //   "CONTENT_STAGED_JSON_ESCAPED_UTF8_BYTES": [{"data": "125840211"}]
        //
        // These fields must be written as siblings of the original field array, not as values inside
        // it. Because this method streams JSON tokens, it has to remember the current array field name
        // and any staged text metadata until the END_ARRAY token has been copied to the output.
        String pendingFieldName = null;
        final Deque<String> fieldArrayNames = new ArrayDeque<>();
        final Deque<List<ExtractedTextFieldMetadata>> fieldMetadata = new ArrayDeque<>();
        boolean writePendingMetadataAfterCurrentEvent = false;
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
                        pendingFieldName = parser.getText();
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
                        if (dataBuffer != null && dataBuffer.getBytes(StandardCharsets.UTF_8).length > fieldValueSizeThreshold) {
                            // write it out to a loose file
                            final String fileName = RandomStringUtils.randomAlphanumeric(10);
                            final StagedFieldData stagedFieldData
                                = writeDataToFile(dataBuffer, fileName, inprogressContentFolderPath, encodingBuffer);
                            final String currentFieldName = fieldArrayNames.isEmpty() ? null : fieldArrayNames.peek();
                            LOGGER.info("Staged field value. Field: {}; File: {}; Metadata recorded: {}",
                                        currentFieldName, stagedFieldData.fileName(), stagedFieldData.metadata() != null);

                            // writeDataToFile() returns the generated file name rather than the full
                            // storage reference. At this point we only know the local staged file name,
                            // for example:
                            //
                            //   abc123.txt
                            //
                            // The storageRefPath is applied later in the existing updateReference block,
                            // where local_ref and newly staged files are both normalized into the final
                            // value written back into JSON:
                            //
                            //   "/etc/store/batches/.../files/abc123.txt"
                            final String contentFileName = stagedFieldData.fileName();

                            // Metadata is present only for UTF-8 text staged by this method. It is null
                            // for base64/binary values because those are written as decoded .bin files and
                            // are not meaningful as JSON-escaped text.
                            //
                            // fieldMetadata is a stack that tracks the field array currently being copied.
                            // When CONTENT is being processed, fieldMetadata.peek() is the metadata list
                            // for that CONTENT array. Adding here records that one of the values in the
                            // array was staged and that sibling metadata fields should be emitted after
                            // the CONTENT array has closed.
                            //
                            // The empty-stack guard is defensive. Valid document fields are arrays, so in
                            // normal validated documents this stack is populated. Keeping the guard avoids
                            // failing hard if minifyJson() is used on looser JSON shapes in tests or tools.
                            if (stagedFieldData.metadata() != null && !fieldMetadata.isEmpty()) {
                                fieldMetadata.peek().add(stagedFieldData.metadata());
                                LOGGER.info("Queued staged text metadata. Field: {}; Raw UTF-8 bytes: {}; JSON-escaped UTF-8 bytes: {}",
                                            currentFieldName,
                                            stagedFieldData.metadata().rawUtf8Bytes(),
                                            stagedFieldData.metadata().jsonEscapedUtf8Bytes());
                            }

                            // Replace the buffered original field value with the staged file name and mark
                            // the field as a storage reference. The original object:
                            //
                            //   {"data": "very large text"}
                            //
                            // is then written out below as:
                            //
                            //   {"data": "/etc/store/batches/.../files/abc123.txt",
                            //    "encoding": "storage_ref"}
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
                case START_ARRAY:
                    // Valid document fields are represented as arrays of field values, for example:
                    //
                    //   "CONTENT": [
                    //     {"data": "first value"},
                    //     {"data": "second value"}
                    //   ]
                    //
                    // If either value is staged, the metadata collected while copying this array is
                    // written immediately after the array ends using the same field name as a prefix.
                    // The stacks handle nested arrays elsewhere in the document while preserving the
                    // field array that owns the staged value.
                    fieldArrayNames.push(pendingFieldName == null ? "" : pendingFieldName);
                    fieldMetadata.push(new ArrayList<>());
                    pendingFieldName = null;
                    break;
                case END_ARRAY:
                    writePendingMetadataAfterCurrentEvent = !fieldMetadata.isEmpty() && !fieldMetadata.peek().isEmpty();
                    break;
                case START_OBJECT:
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                default:
                    break;
            }
            if (!pauseWriting) {
                gen.copyCurrentEvent(parser);
                if (writePendingMetadataAfterCurrentEvent) {
                    // The current token is END_ARRAY. Copying it first closes the original field:
                    //
                    //   "CONTENT": [{"data": "/staged/files/abc123.txt", "encoding": "storage_ref"}]
                    //
                    // Only then can the generated metadata be written as sibling fields:
                    //
                    //   "CONTENT_STAGED_RAW_UTF8_BYTES": [{"data": "7"}],
                    //   "CONTENT_STAGED_JSON_ESCAPED_UTF8_BYTES": [{"data": "10"}]
                    //
                    // If this were written before copying END_ARRAY, the metadata would be emitted
                    // inside the CONTENT array and the document shape would be invalid for consumers.
                    writeMetadataFields(gen, fieldArrayNames.peek(), fieldMetadata.peek());
                    writePendingMetadataAfterCurrentEvent = false;
                }
                if (token == JsonToken.END_ARRAY) {
                    fieldArrayNames.pop();
                    fieldMetadata.pop();
                }
            }
        }
    }

    private static StagedFieldData writeDataToFile(final String data, final String fileName,
                                                   final String inprogressContentFolderPath, final String encoding) throws IOException
    {
        String contentFileName = fileName;
        if (encoding == null || encoding.equalsIgnoreCase(UTF8_ENCODING)) {
            contentFileName = fileName + TXT_ENTENSION;
            final Path targetFile = Paths.get(inprogressContentFolderPath, contentFileName);
            FileUtils.writeStringToFile(targetFile.toFile(), data, StandardCharsets.UTF_8);

            // Record two sizes for staged text:
            //
            //   1. rawUtf8Bytes is the number of bytes written to the staged .txt file.
            //   2. jsonEscapedUtf8Bytes is the number of UTF-8 bytes that same text may contribute
            //      once a downstream component JSON-escapes it.
            //
            // These differ when the text contains characters that must be escaped in JSON.
            // The raw value is what was written to NFS; the escaped value is what a downstream
            // component may need to budget for if it serializes the same text inside a JSON string.
            //
            //   Raw text                         JSON string content             Size change
            //   abc                              abc                             3 -> 3 bytes
            //   a"b                              a\"b                            3 -> 4 bytes
            //   a\b                              a\\b                            3 -> 4 bytes
            //   a newline b                      a\nb                            3 -> 4 bytes
            //   control character 0x01           backslash-u-0001                1 -> 6 bytes
            //
            // Note that these examples exclude the surrounding JSON quotes and field-name
            // overhead. The metadata describes the staged field value itself, not the full
            // document, batch, message, or request that may later contain it.
            //
            // For example, this Java string:
            //
            //   ab\n"c\d
            //
            // is 7 UTF-8 bytes as staged text, but it is serialized into JSON as:
            //
            //   ab\n\"c\\d
            //
            // which is 10 UTF-8 bytes before any surrounding JSON quotes or field-name overhead.
            // This distinction matters because downstream components may need to reason about the
            // escaped JSON representation rather than the raw text file stored on NFS.
            final ExtractedTextFieldMetadata metadata = new ExtractedTextFieldMetadata(
                data.getBytes(StandardCharsets.UTF_8).length,
                StringEscapeUtils.escapeJson(data).getBytes(StandardCharsets.UTF_8).length);
            LOGGER.info("Wrote staged UTF-8 field data. File: {}; Raw UTF-8 bytes: {}; JSON-escaped UTF-8 bytes: {}",
                        targetFile, metadata.rawUtf8Bytes(), metadata.jsonEscapedUtf8Bytes());
            return new StagedFieldData(contentFileName, metadata);
        } else if (encoding.equalsIgnoreCase(BASE64_ENCODING)) {
            contentFileName = fileName + BINARY_ENTENSION;
            final Path targetFile = Paths.get(inprogressContentFolderPath, contentFileName);
            // If encoding is base64, write file after base64 decoding the data
            final byte[] decodedData = Base64.decodeBase64(data);
            FileUtils.writeByteArrayToFile(targetFile.toFile(), decodedData);
            LOGGER.info("Wrote staged base64 field data. File: {}; Decoded bytes: {}", targetFile, decodedData.length);
        }
        return new StagedFieldData(contentFileName, null);
    }

    private static void writeMetadataFields(final JsonGenerator gen, final String fieldName,
                                            final List<ExtractedTextFieldMetadata> metadata) throws IOException
    {
        if (fieldName == null) {
            return;
        }
        // Metadata is emitted using the source field name plus a stable suffix. For a staged CONTENT
        // value the generated fields are:
        //
        //   "CONTENT_STAGED_RAW_UTF8_BYTES": [{"data": "..."}]
        //   "CONTENT_STAGED_JSON_ESCAPED_UTF8_BYTES": [{"data": "..."}]
        //
        // The values are modelled as ordinary field-value arrays so downstream components can read
        // them through the same field access patterns they already use for other fields.
        LOGGER.info("Writing staged text metadata fields. Field: {}; Staged values: {}", fieldName, metadata.size());
        writeMetadataField(gen, fieldName + RAW_UTF8_BYTES_SUFFIX, metadata, true);
        writeMetadataField(gen, fieldName + JSON_ESCAPED_UTF8_BYTES_SUFFIX, metadata, false);
    }

    private static void writeMetadataField(final JsonGenerator gen, final String fieldName,
                                           final List<ExtractedTextFieldMetadata> metadata,
                                           final boolean rawUtf8Bytes) throws IOException
    {
        // Preserve the document-field shape: every metadata value is an object containing a data
        // property inside an array. This mirrors normal field JSON:
        //
        //   "FIELD_NAME": [{"data": "value"}]
        //
        // If a multivalued field has more than one staged value, this writes one metadata value per
        // staged value in encounter order.
        gen.writeFieldName(fieldName);
        gen.writeStartArray();
        for (final ExtractedTextFieldMetadata value : metadata) {
            gen.writeStartObject();
            gen.writeStringField(DATA_FIELD, Long.toString(rawUtf8Bytes ? value.rawUtf8Bytes() : value.jsonEscapedUtf8Bytes()));
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    // Bundles the file written for a staged field value with optional size metadata about that
    // value. The metadata is optional because binary/base64 content is staged as bytes, not as
    // text that will later be JSON-escaped.
    private record StagedFieldData(String fileName, ExtractedTextFieldMetadata metadata)
    {
    }

    // Captures the two byte counts needed to reason about a staged text value:
    //
    //   rawUtf8Bytes:             bytes stored in the .txt file on NFS
    //   jsonEscapedUtf8Bytes:     bytes after JSON escaping, before surrounding request overhead
    //
    // Both values are retained because raw file size alone can undercount the eventual JSON
    // request contribution when the text contains quotes, backslashes, control characters, or
    // other characters that expand during JSON escaping.
    private record ExtractedTextFieldMetadata(long rawUtf8Bytes, long jsonEscapedUtf8Bytes)
    {
    }
}
