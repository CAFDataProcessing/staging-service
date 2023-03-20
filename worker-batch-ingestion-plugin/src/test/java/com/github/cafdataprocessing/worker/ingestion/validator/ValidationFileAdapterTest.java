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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationFileAdapterTest
{
    private static final String AGENT_TEST_FILE_1 = "validator/agentFields-test1.json";

    @Test
    public void testGetFieldKeys() throws IOException
    {
        final int expectedNumberOfFields = 14;

        final ValidationFileAdapter adapter = new ValidationFileAdapter(AGENT_TEST_FILE_1);
        final ArrayList<String> fieldKeys = adapter.getFieldKeys();

        assertEquals(expectedNumberOfFields, fieldKeys.size());
    }

    @Test
    public void getFileContentsTestAdapterException()
    {
        final String fakeFilePath = "FAKE_FILE_PATH";
        final Exception exception = assertThrows(IOException.class,
                                                 () -> new ValidationFileAdapter(fakeFilePath));

        final String expectedMessage = "Failed to read Validation File: " + fakeFilePath;
        final String actualMessage = exception.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }
}
