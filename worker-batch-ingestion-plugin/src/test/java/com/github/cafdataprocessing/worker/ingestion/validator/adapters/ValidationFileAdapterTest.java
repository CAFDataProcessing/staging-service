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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationFileAdapterTest
{
    private static final String AGENT_TEST_FILE = "target/test-classes/validator/agentFields-test2.json";

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
    public void getFileContentsTestSuccess() throws ValidationFileAdapterException
    {
        final String result = ValidationFileAdapter.getFileContents(AGENT_TEST_FILE);

        assertEquals(AGENT_TEST_FILE_CONTENTS, result);
    }

    @Test
    public void getFileContentsTestAdapterException()
    {
        final String fakeFilePath = "FAKE_FILE_PATH";
        final Exception exception = assertThrows(ValidationFileAdapterException.class,
                                                 () -> ValidationFileAdapter.getFileContents(fakeFilePath));

        final String expectedMessage = "ValidationFileAdapterException: Failed to get file contents from "
            + fakeFilePath;
        final String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }
}
