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

import java.io.IOException;
import java.util.List;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class ValidationFileAdapterTest
{
    private static final String AGENT_TEST_FILE_1 = "src/test/resources/validator/agentFields-test1.json";

    @Test
    public void testGetFieldKeys() throws IOException
    {
        final int expectedNumberOfFields = 14;
        final List<?> fieldKeys = ValidationFileAdapter.getFieldNamePatterns(AGENT_TEST_FILE_1);

        assertEquals(expectedNumberOfFields, fieldKeys.size());
    }

    @Test
    public void getFileContentsTestAdapterException()
    {
        final String fakeFilePath = "FAKE_FILE_PATH";
        final Exception exception = assertThrows(IOException.class,
                                                 () -> ValidationFileAdapter.getFieldNamePatterns(fakeFilePath));

        final String actualMessage = exception.getMessage();

        assertThat(actualMessage, containsString(fakeFilePath));
    }
}
