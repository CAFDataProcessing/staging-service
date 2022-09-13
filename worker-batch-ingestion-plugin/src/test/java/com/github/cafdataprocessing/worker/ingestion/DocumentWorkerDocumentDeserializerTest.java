package com.github.cafdataprocessing.worker.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentWorkerDocumentDeserializerTest {

    @Test
    void deserialize() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(DocumentWorkerDocument.class, new DocumentWorkerDocumentDeserializer(100));
        objectMapper.registerModule(simpleModule);

        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();
        documentWorkerDocument.fields = new HashMap<>();
        documentWorkerDocument.fields.put("field", getDocumentWorkerFieldValues());

        documentWorkerDocument.failures = new ArrayList<>();
        documentWorkerDocument.failures.add(getDocumentWorkerFailure());

        documentWorkerDocument.subdocuments = new ArrayList<>();
        for(int i = 0; i <1000; i ++){
            documentWorkerDocument.reference = String.format("r%s", i);
            final DocumentWorkerDocument subdocument = new DocumentWorkerDocument();
            subdocument.reference = "subdocument " + i;
            subdocument.fields = new HashMap<>();
            subdocument.fields.put("myfield", getDocumentWorkerFieldValues());

            subdocument.failures = new ArrayList<>();
            subdocument.failures.add(getDocumentWorkerFailure());
            
            documentWorkerDocument.subdocuments.add(subdocument);
        }

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = documentWorkerDocument;

        final String json = objectMapper.writeValueAsString(documentWorkerDocumentTask);
        
        final DocumentWorkerDocumentTask deserialisedDocumentWorkerDocumentTask = 
                objectMapper.readValue(json, DocumentWorkerDocumentTask.class);
        
        assertEquals(100, deserialisedDocumentWorkerDocumentTask.document.subdocuments.size());
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
}
