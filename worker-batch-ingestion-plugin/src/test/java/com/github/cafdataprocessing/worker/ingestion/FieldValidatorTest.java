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

import com.github.cafdataprocessing.worker.ingestion.validator.FieldValidator;
import com.github.cafdataprocessing.worker.ingestion.validator.adapters.AdapterException;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FieldValidatorTest {
    final static String FAILURE_MESSAGE_SUFFIX = " is not allowed to be set by the agent";

    @Test
    public void testFieldValidatorAgentFields() throws AdapterException
    {
        final FieldValidator agentFieldValidator = new FieldValidator("target/test-classes/testAgentFields.json");

        final Map<String, List<DocumentWorkerFieldValue>> documentFields = new HashMap<>();
        documentFields.put("ACCOUNTS", Collections.singletonList(new DocumentWorkerFieldValue()));
        documentFields.put("COLLECTION_STATUS", Collections.singletonList(new DocumentWorkerFieldValue()));
        documentFields.put("INVALID_FIELD", Collections.singletonList(new DocumentWorkerFieldValue()));
        DocumentWorkerDocument document = createDocument(documentFields);

        document = agentFieldValidator.validate(document);

        assertEquals(2, document.fields.size());
        assertFalse(document.fields.containsKey("INVALID_FIELD"));
        assertEquals("FIELD-NOT-ALLOWED-FAILURE", document.failures.get(0).failureId);
        assertEquals("INVALID_FIELD" + FAILURE_MESSAGE_SUFFIX,
                document.failures.get(0).failureMessage);
    }

    private DocumentWorkerDocument createDocument(Map<String, List<DocumentWorkerFieldValue>> documentFields)
    {
        final DocumentWorkerDocument document = new DocumentWorkerDocument();
        document.fields = documentFields;

        return document;
    }
}
