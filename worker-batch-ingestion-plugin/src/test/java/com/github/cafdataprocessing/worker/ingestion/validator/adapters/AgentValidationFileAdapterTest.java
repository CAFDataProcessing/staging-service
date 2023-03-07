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

public class AgentValidationFileAdapterTest
{
    private static final String AGENT_TEST_FILE = "target/test-classes/validator/agentFields-test1.json";

    @Test
    public void testGetFieldKeys() throws ValidationFileAdapterException
    {
        final int expectedNumberOfFields = 10;

        final AgentValidationFileAdapter adapter = new AgentValidationFileAdapter(AGENT_TEST_FILE);
        final Set<String> fieldKeys = adapter.getFieldKeys();

        assertEquals(expectedNumberOfFields, fieldKeys.size());
    }

    @Test
    public void testGetFlattenedFieldKeysOcr() throws ValidationFileAdapterException
    {
        final AgentValidationFileAdapter adapter = new AgentValidationFileAdapter(AGENT_TEST_FILE);
        final Map<String, Set<String>> flattenedFields = adapter.getFlattenedFieldKeys();

        assertTrue(flattenedFields.get("OCR").contains("CONFIDENCE"));
        assertEquals(4, flattenedFields.get("OCR").size());
    }

    @Test
    public void testGetFlattenedFieldKeysMetadataFiles() throws ValidationFileAdapterException
    {
        final AgentValidationFileAdapter adapter = new AgentValidationFileAdapter(AGENT_TEST_FILE);
        final Map<String, Set<String>> flattenedFields = adapter.getFlattenedFieldKeys();

        assertTrue(flattenedFields.get("METADATA_FILES").contains("CONTENT"));
        assertEquals(2, flattenedFields.get("METADATA_FILES").size());
    }
}
