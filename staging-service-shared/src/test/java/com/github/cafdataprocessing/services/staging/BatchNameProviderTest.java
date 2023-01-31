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
package com.github.cafdataprocessing.services.staging;

import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchNameProvider;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

public final class BatchNameProviderTest
{
    @Test
    void testValidBatchId()
    {
        try {
            final String id = "abc-xyz";
            final BatchId bid = new BatchId(id);
            final String batchName = BatchNameProvider.getBatchDirectoryName(bid);
            System.out.println("testValidBatchId: " + batchName);
            final String batchId = BatchNameProvider.getBatchId(batchName);
            assertEquals(bid.getValue(), batchId);
        } catch (final InvalidBatchIdException e) {
            fail("InvalidBatchIdException" + e);
        }
    }

    @Test
    void testValidBatchIdLikeServiceId()
    {
        try {
            final String id = ServiceIdentifier.getServiceId();
            final BatchId bid = new BatchId(id);
            final String batchName = BatchNameProvider.getBatchDirectoryName(bid);
            System.out.println("testValidBatchIdLikeServiceId: " + batchName);
            final String batchId = BatchNameProvider.getBatchId(batchName);
            assertEquals(bid.getValue(), batchId);
        } catch (final InvalidBatchIdException e) {
            fail("InvalidBatchIdException" + e);
        }
    }

    @Test
    void testValidLongerBatchId()
    {
        try {
            final String id = "pqr-abc-xyz";
            final BatchId bid = new BatchId(id);
            final String batchName = BatchNameProvider.getBatchDirectoryName(bid);
            System.out.println("testValidLongerBatchId: " + batchName);
            final String batchId = BatchNameProvider.getBatchId(batchName);
            assertEquals(bid.getValue(), batchId);
        } catch (final InvalidBatchIdException e) {
            fail("InvalidBatchIdException" + e);
        }
    }

    @Test
    void testInvalidBatchId()
    {
        assertThrows(
            InvalidBatchIdException.class,
            () -> new BatchId("abc.."),
            "Expected InvalidBatchIdException"
        );
    }
}
