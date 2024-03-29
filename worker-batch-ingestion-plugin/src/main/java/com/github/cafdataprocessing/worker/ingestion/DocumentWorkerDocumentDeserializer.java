/*
 * Copyright 2019-2024 Open Text.
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
package com.github.cafdataprocessing.worker.ingestion;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.mutable.MutableInt;

public class DocumentWorkerDocumentDeserializer extends StdDeserializer<DocumentWorkerDocument>
{
    private final int totalSubdocumentLimit;

    public DocumentWorkerDocumentDeserializer(final int totalSubdocumentLimit)
    {
        this(null, totalSubdocumentLimit);
    }

    public DocumentWorkerDocumentDeserializer(final Class<?> vc, final int totalSubdocumentLimit)
    {
        super(vc);
        this.totalSubdocumentLimit = totalSubdocumentLimit;
    }

    @Override
    public DocumentWorkerDocument deserialize(
        final JsonParser jsonParser,
        final DeserializationContext deserializationContext
    ) throws IOException, JsonProcessingException
    {
        final MutableInt totalSubdocuments = new MutableInt();

        final DocumentWorkerDocument documentWorkerDocument
            = deserializeDocumentWorkerDocument(jsonParser, deserializationContext, totalSubdocuments);

        if (totalSubdocuments.intValue() > this.totalSubdocumentLimit) {
            if (documentWorkerDocument.failures == null) {
                documentWorkerDocument.failures = new ArrayList<>();
            }
            final DocumentWorkerFailure subdocumentsTruncatedFailure = new DocumentWorkerFailure();

            subdocumentsTruncatedFailure.failureId = "IBWP-SUBDOCUMENTS_TRUNCATED-WARNING";
            subdocumentsTruncatedFailure.failureMessage
                = String.format("Subdocuments were truncated at %s", totalSubdocumentLimit);
            subdocumentsTruncatedFailure.failureStack = Arrays.toString(Thread.currentThread().getStackTrace());

            documentWorkerDocument.failures.add(subdocumentsTruncatedFailure);
        }

        return documentWorkerDocument;
    }

    private DocumentWorkerDocument deserializeDocumentWorkerDocument(
        final JsonParser jsonParser,
        final DeserializationContext deserializationContext,
        final MutableInt totalSubdocuments
    ) throws IOException
    {
        if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException(
                String.format("Expected '{' at %s", jsonParser.getCurrentLocation().toString()));
        }

        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();

        jsonParser.nextToken();

        while (jsonParser.currentToken() != JsonToken.END_OBJECT) {
            if (jsonParser.currentToken() != JsonToken.FIELD_NAME) {
                //If the current token isn't a field name then json parsing isn't working as expected
                throw new IllegalStateException(
                        String.format("Expected 'Field' at %s", jsonParser.getCurrentLocation().toString()));
            }
            final String field = jsonParser.getCurrentName();
            jsonParser.nextToken();
            switch (field) {
                case "reference": {
                    documentWorkerDocument.reference = jsonParser.getValueAsString();
                    break;
                }
                case "fields": {
                    documentWorkerDocument.fields = deserializationContext.readValue(
                            jsonParser,
                            deserializationContext.getTypeFactory()
                                .constructType(
                                    new TypeReference<Map<String, List<DocumentWorkerFieldValue>>>()
                                {
                                }));
                    break;
                }
                case "failures": {
                    documentWorkerDocument.failures = deserializationContext.readValue(
                            jsonParser,
                            deserializationContext.getTypeFactory()
                                    .constructType(new TypeReference<List<DocumentWorkerFailure>>()
                                    {
                                    }));
                    break;
                }
                case "subdocuments": {
                    documentWorkerDocument.subdocuments =
                            deserializeSubdocuments(jsonParser, deserializationContext, totalSubdocuments);
                    break;
                }
                default: {
                    throw new IllegalStateException(
                            String.format("Unexpected field '%s' at %s", field, jsonParser.getCurrentLocation().toString()));
                }
            }
            jsonParser.nextToken();
        }

        return documentWorkerDocument;
    }

    private List<DocumentWorkerDocument> deserializeSubdocuments(
        final JsonParser jsonParser,
        final DeserializationContext deserializationContext,
        final MutableInt totalSubdocuments
    ) throws IOException
    {
        if (jsonParser.currentToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException(String.format("Expected '[' at %s",
                                                          jsonParser.getCurrentLocation().toString()));
        }

        jsonParser.nextToken();

        final List<DocumentWorkerDocument> documentWorkerSubdocumentList = new ArrayList<>();

        while (jsonParser.currentToken() != JsonToken.END_ARRAY) {

            totalSubdocuments.increment();

            if (totalSubdocuments.intValue() <= totalSubdocumentLimit) {
                documentWorkerSubdocumentList.add(
                    deserializeDocumentWorkerDocument(jsonParser, deserializationContext,
                                                      totalSubdocuments));
            }
            else {
                jsonParser.skipChildren(); //Skip this document and it's children
            }
            jsonParser.nextToken();
        }

        return documentWorkerSubdocumentList;
    }
}
