package com.github.cafdataprocessing.worker.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DocumentWorkerDocumentDeserializerTest {

    @Test
    void deserialize() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(DocumentWorkerDocument.class, new DocumentWorkerDocumentDeserializer());
        objectMapper.registerModule(simpleModule);

        final DocumentWorkerDocument documentWorkerDocument = new DocumentWorkerDocument();
        documentWorkerDocument.subdocuments = new ArrayList<>();
        for(int i = 0; i <1000; i ++){
            documentWorkerDocument.reference = String.format("r%s",i);
            documentWorkerDocument.subdocuments.add(new DocumentWorkerDocument());
        }

        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        documentWorkerDocumentTask.document = documentWorkerDocument;

        final String json = objectMapper.writeValueAsString(documentWorkerDocumentTask);
        
        final DocumentWorkerDocumentTask deserialisedDocumentWorkerDocumentTask = 
                objectMapper.readValue(json, DocumentWorkerDocumentTask.class);
        
        assertEquals(100, deserialisedDocumentWorkerDocumentTask.document.subdocuments.size());
    }
}
