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
package com.github.cafdataprocessing.worker.ingestion.validator;

import com.github.cafdataprocessing.worker.ingestion.validator.adapters.AdapterException;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldValidatorTest
{
    private final static String AGENT_TEST_FILE = "target/test-classes/validator/agentFields-test1.json";
    private final static String AGENT_FAILURE_MESSAGE_SUFFIX = " is not allowed to be set by the agent";

    @Test
    public void testFieldValidatorAgentFields() throws AdapterException
    {
        final int expectedFields = 2;
        final String fieldName = "INVALID_FIELD";

        final List<String> fieldNames = Arrays.asList("ACCOUNTS", "COLLECTION_STATUS", fieldName);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        DocumentWorkerDocument cleanDoc = agentFieldValidator.validate(document);

        assertEquals(expectedFields, cleanDoc.fields.size());
        assertFalse(document.fields.containsKey("INVALID_FIELD"));
        assertEquals("FIELD-NOT-ALLOWED-FAILURE", cleanDoc.failures.get(0).failureId);
        assertEquals(fieldName + AGENT_FAILURE_MESSAGE_SUFFIX,
                     cleanDoc.failures.get(0).failureMessage);
    }

    @Test
    public void testFieldValidatorFlattenedAgentFields() throws AdapterException
    {
        final int expectedFields = 3;
        final String fieldName = "METADATA_FILES_0_CONTENT";

        List<String> fieldNames = Arrays.asList("ACCOUNTS", "COLLECTION_STATUS", fieldName);
        DocumentWorkerDocument document = createDocument(createDocumentFields(fieldNames));

        final FieldValidator agentFieldValidator = new FieldValidator(AGENT_TEST_FILE);
        document = agentFieldValidator.validate(document);

        assertEquals(expectedFields, document.fields.size());
        assertTrue(document.fields.containsKey(fieldName));
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
