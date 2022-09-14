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
            = deserializeDocumentWorkerDocument(
                jsonParser, deserializationContext, jsonParser.currentToken(), totalSubdocuments);

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
        final JsonToken currentJsonToken,
        final MutableInt totalSubdocuments
    ) throws IOException
    {
        if (currentJsonToken != JsonToken.START_OBJECT) {
            throw new IllegalStateException(
                String.format("Expected '{' at %s", jsonParser.getCurrentLocation().toString()));
        }

        jsonParser.nextToken();

        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();

        while (jsonParser.currentToken() != JsonToken.END_OBJECT) {
            if (jsonParser.currentToken() == JsonToken.FIELD_NAME) {
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
                        documentWorkerDocument.subdocuments
                            = deserializeSubdocuments(jsonParser, deserializationContext, jsonParser.currentToken(),
                                                      totalSubdocuments);
                        break;
                    }
                    default: {
                        throw new IllegalStateException(
                            String.format("Unexpected field '%s' at %s", field, jsonParser.getCurrentLocation().toString()));
                    }
                }
            }
            jsonParser.nextToken();
        }

        return documentWorkerDocument;
    }

    private List<DocumentWorkerDocument> deserializeSubdocuments(
        final JsonParser jsonParser,
        final DeserializationContext deserializationContext,
        final JsonToken currentJsonToken,
        final MutableInt totalSubdocuments
    ) throws IOException
    {
        if (currentJsonToken != JsonToken.START_ARRAY) {
            throw new IllegalStateException(String.format("Expected '[' at %s",
                                                          jsonParser.getCurrentLocation().toString()));
        }

        final List<DocumentWorkerDocument> documentWorkerSubdocumentList = new ArrayList<>();

        JsonToken currentSubdocumentArrayJsonToken = jsonParser.nextToken();

        while (currentSubdocumentArrayJsonToken != JsonToken.END_ARRAY) {

            final int currentCount = totalSubdocuments.incrementAndGet();

            if (currentCount <= totalSubdocumentLimit) {
                documentWorkerSubdocumentList.add(
                    deserializeDocumentWorkerDocument(jsonParser, deserializationContext,
                                                      currentSubdocumentArrayJsonToken,
                                                      totalSubdocuments));
            }

            currentSubdocumentArrayJsonToken = jsonParser.nextToken();

            if (currentCount > totalSubdocumentLimit) {
                jsonParser.skipChildren();
                totalSubdocuments.increment();
                break;
            }
        }

        return documentWorkerSubdocumentList;
    }
}
