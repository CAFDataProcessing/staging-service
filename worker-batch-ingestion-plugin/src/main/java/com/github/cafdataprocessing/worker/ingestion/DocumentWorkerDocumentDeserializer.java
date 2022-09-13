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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentWorkerDocumentDeserializer extends StdDeserializer<DocumentWorkerDocument> {

    public DocumentWorkerDocumentDeserializer() {
        this(null);
    }

    public DocumentWorkerDocumentDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public DocumentWorkerDocument deserialize(final JsonParser jsonParser, 
                                              final DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {

        return deserializeDocumentWorkerDocument(jsonParser, jsonParser.currentToken(), new MutableInt());

    }
    private DocumentWorkerDocument deserializeDocumentWorkerDocument(
            final JsonParser jsonParser,
            final JsonToken currentJsonToken,
            final MutableInt totalSubdocuments) throws IOException {

        if(currentJsonToken != JsonToken.START_OBJECT){
            throw new IllegalStateException("TODO");
        }

        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();

        JsonToken currentDocumentJsonToken = jsonParser.nextToken();

        while(currentDocumentJsonToken != JsonToken.END_OBJECT) {
            if(currentDocumentJsonToken == JsonToken.FIELD_NAME){
                final String field = jsonParser.getCurrentName();
                switch(field){
                    case "reference": {
                        documentWorkerDocument.reference = jsonParser.getValueAsString();
                        break;
                    }
                    case "fields": {
                        documentWorkerDocument.fields = deserializeFields(jsonParser, jsonParser.nextToken());
                        break;
                    }
                    case "failures": {
                        documentWorkerDocument.failures = deserializeFailures(jsonParser, jsonParser.nextToken());
                        break;
                    }
                    case "subdocuments": {
                        documentWorkerDocument.subdocuments =
                                deserializeSubdocuments(jsonParser, jsonParser.nextToken(), totalSubdocuments);
                        break;
                    }
                } 
            }
            currentDocumentJsonToken = jsonParser.nextToken();
        }

        return documentWorkerDocument;

    }
    
    private Map<String, List<DocumentWorkerFieldValue>> deserializeFields(final JsonParser jsonParser, 
                                                                          final JsonToken currentJsonToken) 
            throws IOException 
    {

        if(currentJsonToken != JsonToken.START_ARRAY){
            throw new IllegalStateException("TODO");
        }

        final Map<String, List<DocumentWorkerFieldValue>> fields = new HashMap<>();

        JsonToken currentFieldArrayJsonToken = jsonParser.nextToken();

        while(currentFieldArrayJsonToken != JsonToken.END_ARRAY) {
            fi

        }
        
        return fields;
    }

    private List<DocumentWorkerFailure> deserializeFailures(final JsonParser jsonParser,
                                                            final JsonToken currentJsonToken) throws IOException {

        final List<DocumentWorkerFailure> documentWorkerFailures = new ArrayList<>();

        jsonParser.skipChildren();

        return documentWorkerFailures;
    }

    private List<DocumentWorkerDocument> deserializeSubdocuments(final JsonParser jsonParser,
                                                                 final JsonToken currentJsonToken,
                                                                 final MutableInt totalSubdocuments) throws IOException 
    {

        if(currentJsonToken != JsonToken.START_ARRAY){
            throw new IllegalStateException("TODO");
        }

        final List<DocumentWorkerDocument> documentWorkerSubdocumentList = new ArrayList<>();

        JsonToken currentSubdocumentArrayJsonToken = jsonParser.nextToken();

        while(currentSubdocumentArrayJsonToken != JsonToken.END_ARRAY) {

            if(totalSubdocuments.intValue() >= 100) {
                jsonParser.skipChildren();
                break;
            }

            documentWorkerSubdocumentList.add(deserializeDocumentWorkerDocument(jsonParser, 
                    currentSubdocumentArrayJsonToken, 
                    totalSubdocuments));
            
            totalSubdocuments.increment();
            currentSubdocumentArrayJsonToken = jsonParser.nextToken();

        }

        return documentWorkerSubdocumentList;
    }
}
