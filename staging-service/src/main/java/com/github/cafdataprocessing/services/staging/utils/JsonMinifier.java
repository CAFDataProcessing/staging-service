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
package com.github.cafdataprocessing.services.staging.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class JsonMinifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMinifier.class);

    private static final String DATA_FIELD = "data";
    private static final String ENCODING_FIELD = "encoding";
    private static final String LOCAL_REF = "local_ref";
    private static final String STORAGE_REF = "storage_ref";

    private JsonMinifier(){}

    public static final void minifyJson(final InputStream inputStream, final OutputStream outstream, final String storageRefPath) throws IOException
    {
        final JsonFactory factory = new JsonFactory();
        factory.configure(Feature.FLUSH_PASSED_TO_STREAM, false);
        factory.configure(Feature.AUTO_CLOSE_TARGET, false);
        final JsonParser parser = factory.createParser(inputStream);
        String dataBuffer = null;
        String encodingBuffer = null;
        JsonToken token;
        try(final JsonGenerator gen = factory.createGenerator(outstream))
        {
            boolean pauseWriting = false;
            boolean bufferData = false;
            boolean bufferEncoding = false;
            boolean updateReference = false;

            while ((token = parser.nextToken()) != null) {
                switch (token) {
                    case FIELD_NAME:
                        LOGGER.trace("Read FIELD_NAME...");
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
                        LOGGER.trace("Read VALUE...");
                        // Buffer data or encoding field values
                        if (bufferData) {
                            dataBuffer = parser.getText();
                        }
                        if (bufferEncoding) {
                            encodingBuffer = parser.getText();
                            if (encodingBuffer.equalsIgnoreCase(LOCAL_REF)) {
                                encodingBuffer = STORAGE_REF;
                                updateReference = true;
                            } else {
                                updateReference = false;
                            }
                        }
                        break;
                    case END_OBJECT:
                        LOGGER.trace("Read END_OBJECT...");
                        // If writing was paused, update buffered data/encoding
                        // values and write them out
                        if (pauseWriting) {
                            if (updateReference) {
                                dataBuffer = storageRefPath + "/" + dataBuffer;
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
                        pauseWriting = false;
                        break;
                    case START_OBJECT:
                    case START_ARRAY:
                    case END_ARRAY:
                    case NOT_AVAILABLE:
                    case VALUE_EMBEDDED_OBJECT:
                    default:
                        break;
                }
                LOGGER.trace(parser.getText());
                if (!pauseWriting) {
                    gen.copyCurrentEvent(parser);
                }
            }
        }
        outstream.write('\n');
    }

}
