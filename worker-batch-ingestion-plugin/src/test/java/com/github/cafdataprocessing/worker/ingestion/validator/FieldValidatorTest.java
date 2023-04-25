/*
 * Copyright 2019-2023 Open Text.
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
package com.github.cafdataprocessing.worker.ingestion.validator;

import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class FieldValidatorTest
{
    private final static String AGENT_TEST_FILE = "src/test/resources/validator/agentFields-test1.json";
    private final static String INVALID_FIELD_NAME = "INVALID_FIELD_NAME";

    @Test
    void testFieldValidatorAgentFields() throws IOException
    {
        final int expectedFields = 2;

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", "COLLECTION_STATUS", INVALID_FIELD_NAME);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        final DocumentWorkerDocument cleanDoc = agentFieldValidator.validate(document);

        assertEquals(expectedFields, cleanDoc.fields.size());
        assertFalse(document.fields.containsKey("INVALID_FIELD"));
        assertEquals("IW-001", cleanDoc.failures.get(0).failureId);
        assertThat(cleanDoc.failures.get(0).failureMessage, containsString(INVALID_FIELD_NAME));
    }

    @Test
    void testFieldValidatorFlattenedAgentFieldsMetadataFiles() throws IOException
    {
        final int expectedFields = 3;
        final String fieldName = "METADATA_FILES_0_CONTENT";

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", "COLLECTION_STATUS", fieldName);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        agentFieldValidator.validate(document);

        assertEquals(expectedFields, document.fields.size());
        assertTrue(document.fields.containsKey(fieldName));
    }

    @Test
    void testFieldValidatorFlattenedAgentFieldsOcr() throws IOException
    {
        final int expectedFields = 3;
        final String fieldName = "OCR_3_5_CONFIDENCE";

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", "COLLECTION_STATUS", fieldName);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        agentFieldValidator.validate(document);

        assertEquals(expectedFields, document.fields.size());
        assertTrue(document.fields.containsKey(fieldName));
    }

    @Test
    void testFieldValidatorMultipleInvalidFields() throws IOException
    {
        final int expectedFields = 1;
        final int expectedDocumentFailures = 2;

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", "INVALID_FIELD_1", "INVALID_FIELD_2");
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        agentFieldValidator.validate(document);

        assertEquals(expectedFields, document.fields.size());
        assertEquals(expectedDocumentFailures, document.failures.size());
    }

    @Test
    void testFieldValidatorAgentFieldsWithSubDocument() throws IOException
    {
        final int expectedSubDocFields = 1;

        final DocumentWorkerDocument document = createDocument(createDocumentFields(Collections.singletonList("ACCOUNTS")));

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", "COLLECTION_STATUS", INVALID_FIELD_NAME);
        final DocumentWorkerDocument subDocument = createDocument(createDocumentFields(fieldNames));
        document.subdocuments = new ArrayList<>();
        document.subdocuments.add(subDocument);

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        final DocumentWorkerDocument cleanDoc = agentFieldValidator.validate(document);

        assertEquals(expectedSubDocFields, cleanDoc.fields.size());
        assertFalse(document.fields.containsKey(INVALID_FIELD_NAME));
    }

    @Test
    void testFieldValidatorAgentFieldsWithImmutableListOfFailures() throws IOException
    {
        final int expectedFields = 1;
        final int expectedDocumentFailures = 1;

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", INVALID_FIELD_NAME);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        // Make list of failures immutable to invoke UnsupportedOperationException
        document.failures = Collections.unmodifiableList(new ArrayList<>());

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        agentFieldValidator.validate(document);

        assertEquals(expectedFields, document.fields.size());
        assertEquals(expectedDocumentFailures, document.failures.size());
    }

    @Test
    public void testFieldValidatorAgentFieldsWithImmutableMapOfFields() throws IOException
    {
        final int expectedFields = 1;
        final int expectedDocumentFailures = 1;

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", INVALID_FIELD_NAME);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        // Make map of fields immutable to invoke UnsupportedOperationException
        document.fields = Collections.unmodifiableMap(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        agentFieldValidator.validate(document);

        assertEquals(expectedFields, document.fields.size());
        assertEquals(expectedDocumentFailures, document.failures.size());
    }

    private Map<String, List<DocumentWorkerFieldValue>> createDocumentFields(final List<String> fieldNames)
    {
        final Map<String, List<DocumentWorkerFieldValue>> fields = new HashMap<>();
        for (String name : fieldNames) {
            fields.put(name, Collections.singletonList(new DocumentWorkerFieldValue()));
        }
        return fields;
    }

    private DocumentWorkerDocument createDocument(Map<String, List<DocumentWorkerFieldValue>> documentFields)
    {
        final DocumentWorkerDocument document = new DocumentWorkerDocument();
        document.fields = documentFields;

        return document;
    }
}
