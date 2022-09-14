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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DocumentWorkerDocumentDeserializerTest {

    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void before() {
        objectMapper = new ObjectMapper();
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(DocumentWorkerDocument.class, new DocumentWorkerDocumentDeserializer(100));
        objectMapper.registerModule(simpleModule);
    }
    
    @Test
    public void deserializeAndLimitTest() throws JsonProcessingException {

        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();

        documentWorkerDocument.reference = "root";

        documentWorkerDocument.fields = new HashMap<>();
        documentWorkerDocument.fields.put("field", getDocumentWorkerFieldValues());

        documentWorkerDocument.failures = new ArrayList<>();
        documentWorkerDocument.failures.add(getDocumentWorkerFailure());

        documentWorkerDocument.subdocuments = new ArrayList<>();

        for(int subdocumentIndex = 0; subdocumentIndex < 100; subdocumentIndex ++) {
            final DocumentWorkerDocument subdocument = new DocumentWorkerDocument();
            subdocument.reference = documentWorkerDocument.reference + "/subdocument_" + subdocumentIndex;
            
            subdocument.fields = new HashMap<>();
            subdocument.fields.put("myfield", getDocumentWorkerFieldValues());

            subdocument.failures = new ArrayList<>();
            subdocument.failures.add(getDocumentWorkerFailure());

            subdocument.subdocuments = new ArrayList<>();

            documentWorkerDocument.subdocuments.add(subdocument);

            for(int subsubdocumentIndex = 0; subsubdocumentIndex < 100; subsubdocumentIndex ++) {
                final DocumentWorkerDocument subsubdocument = new DocumentWorkerDocument();
                subsubdocument.reference = subdocument.reference + "/subdocument_" + subsubdocumentIndex;
                subdocument.subdocuments.add(subsubdocument);
            }
        }

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = documentWorkerDocument;

        final String json = objectMapper.writeValueAsString(documentWorkerDocumentTask);
        
        final DocumentWorkerDocumentTask deserialisedDocumentWorkerDocumentTask = 
                objectMapper.readValue(json, DocumentWorkerDocumentTask.class);
        
        final MutableInt count = new MutableInt();
        countAllSubdocuments(count, deserialisedDocumentWorkerDocumentTask.document);
        assertEquals(100, count.intValue(), "Sub document count wrong");
        assertEquals(2, deserialisedDocumentWorkerDocumentTask.document.failures.size(), 
                "Failures count incorrect");
        
        final List<DocumentWorkerFailure> actualFailures = 
                deserialisedDocumentWorkerDocumentTask.document.failures.stream()
                .filter(f -> f.failureId.equals("IBWP-SUBDOCUMENTS_TRUNCATED-WARNING")).collect(Collectors.toList());
        
        assertEquals(1, actualFailures.size());
        final DocumentWorkerFailure truncationFailure = actualFailures.get(0);
        assertEquals("Subdocuments were truncated at 100", truncationFailure.failureMessage);
        assertNotNull(truncationFailure.failureStack);
        
    }

    @Test
    void deserializeInvalidDocumentTest() throws JsonProcessingException {

        final String json = "[]";

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
            objectMapper.readValue(json, DocumentWorkerDocument.class);
        });
        Assertions.assertEquals(
                "Expected '{' at [Source: (String)\"[]\"; line: 1, column: 2]",
                thrown.getMessage());

    }

    @Test
    void deserializeInvalidSubdocumentsTest() throws JsonProcessingException {

        final String json = "{\"subdocuments\":{}}";

        final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
            objectMapper.readValue(json, DocumentWorkerDocument.class);
        });
        Assertions.assertEquals(
                "Expected '[' at [Source: (String)\"{\"subdocuments\":{}}\"; line: 1, column: 18]",
                thrown.getMessage());

    }

    private static List<DocumentWorkerFieldValue> getDocumentWorkerFieldValues() {
        final List<DocumentWorkerFieldValue> documentWorkerFieldValueList = new ArrayList<>();
        final DocumentWorkerFieldValue documentWorkerFieldValue = new DocumentWorkerFieldValue();
        documentWorkerFieldValue.data = "Data";
        documentWorkerFieldValue.encoding = DocumentWorkerFieldEncoding.utf8;
        documentWorkerFieldValueList.add(documentWorkerFieldValue);
        return documentWorkerFieldValueList;
    }

    private static DocumentWorkerFailure getDocumentWorkerFailure() {
        final DocumentWorkerFailure documentWorkerFailure = new DocumentWorkerFailure();
        documentWorkerFailure.failureId = "id";
        documentWorkerFailure.failureMessage = "message";
        documentWorkerFailure.failureStack = "stack";
        return documentWorkerFailure;
    }
    
    private void countAllSubdocuments(final MutableInt count, final DocumentWorkerDocument documentWorkerDocument) {
        if (documentWorkerDocument.subdocuments == null) {
            return;
        }
        for (final DocumentWorkerDocument subdocument: documentWorkerDocument.subdocuments) {
            count.increment();
            countAllSubdocuments(count, subdocument);
        }
    }
}
