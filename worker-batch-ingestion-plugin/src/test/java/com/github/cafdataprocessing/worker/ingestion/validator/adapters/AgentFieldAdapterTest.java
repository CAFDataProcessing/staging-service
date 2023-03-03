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
package com.github.cafdataprocessing.worker.ingestion.validator.adapters;


import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentFieldAdapterTest {
    private static final String AGENT_TEST_FILE = "target/test-classes/validator/agentFields-test.json";

    @Test
    public void testGetFieldKeys() throws AdapterException
    {
        final int expectedNumberOfFields = 10;

        final AgentFieldAdapter adapter = new AgentFieldAdapter(AGENT_TEST_FILE);
        final Set<String> fieldKeys = adapter.getFieldKeys();

        assertEquals(expectedNumberOfFields, fieldKeys.size());
    }

    @Test
    public void testGetFlattenedFieldsOcr() throws AdapterException
    {
        final AgentFieldAdapter adapter = new AgentFieldAdapter(AGENT_TEST_FILE);
        final Map<String, Set<String>> flattenedFields = adapter.getFlattenedFields();

        assertTrue(flattenedFields.get("OCR").contains("CONFIDENCE"));
    }

    @Test
    public void testGetFlattenedFieldMetadataFiles() throws AdapterException
    {
        final AgentFieldAdapter adapter = new AgentFieldAdapter(AGENT_TEST_FILE);
        final Map<String, Set<String>> flattenedFields = adapter.getFlattenedFields();

        assertTrue(flattenedFields.get("METADATA_FILES").contains("CONTENT"));
    }
}
