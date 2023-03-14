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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationFileAdapterTest
{
    private static final String AGENT_TEST_FILE_1 = "target/test-classes/validator/agentFields-test1.json";
    private static final String AGENT_TEST_FILE_2 = "target/test-classes/validator/agentFields-test2.json";

    private static final String AGENT_TEST_FILE_CONTENTS
        = "{\n"
        + "  \"types\": {\n"
        + "    \"person\": {\n"
        + "      \"DISPLAY_NAME\": {\n"
        + "        \"ignoreCase\": true,\n"
        + "        \"type\": \"FULLTEXT\"\n"
        + "      }\n"
        + "    }\n"
        + "  },\n"
        + "  \"fields\": {\n"
        + "    \"ACCOUNTS\": {\n"
        + "      \"objectEncoding\": \"json\",\n"
        + "      \"type\": \"person[]\"\n"
        + "    }\n"
        + "  }\n"
        + "}\n";

    @Test
    public void testGetFieldKeys() throws IOException
    {
        final int expectedNumberOfFields = 10;

        final ValidationFileAdapter adapter = new ValidationFileAdapter(AGENT_TEST_FILE_1);
        final Set<String> fieldKeys = adapter.getFieldKeys();

        assertEquals(expectedNumberOfFields, fieldKeys.size());
    }

    @Test
    public void testGetFlattenedFieldKeysOcr() throws IOException
    {
        final ValidationFileAdapter adapter = new ValidationFileAdapter(AGENT_TEST_FILE_1);
        final Map<String, Set<String>> flattenedFields = adapter.getFlattenedFieldKeys();

        assertTrue(flattenedFields.get("OCR").contains("CONFIDENCE"));
        assertEquals(4, flattenedFields.get("OCR").size());
    }

    @Test
    public void testGetFlattenedFieldKeysMetadataFiles() throws IOException
    {
        final ValidationFileAdapter adapter = new ValidationFileAdapter(AGENT_TEST_FILE_1);
        final Map<String, Set<String>> flattenedFields = adapter.getFlattenedFieldKeys();

        assertTrue(flattenedFields.get("METADATA_FILES").contains("CONTENT"));
        assertEquals(2, flattenedFields.get("METADATA_FILES").size());
    }

    @Test
    public void getFileContentsTestSuccess() throws IOException
    {
        final String result = ValidationFileAdapter.getFileContents(AGENT_TEST_FILE_2);

        assertEquals(AGENT_TEST_FILE_CONTENTS, result);
    }

    @Test
    public void getFileContentsTestAdapterException()
    {
        final String fakeFilePath = "FAKE_FILE_PATH";
        final Exception exception = assertThrows(IOException.class,
                                                 () -> ValidationFileAdapter.getFileContents(fakeFilePath));

        final String expectedMessage = "Failed to read Validation File";
        final String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }
}
