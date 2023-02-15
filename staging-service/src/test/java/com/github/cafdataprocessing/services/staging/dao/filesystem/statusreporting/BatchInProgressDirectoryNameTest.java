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
package com.github.cafdataprocessing.services.staging.dao.filesystem.statusreporting;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchNameProvider;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class BatchInProgressDirectoryNameTest
{
    @Test
    public void testValidBatchId() throws InvalidBatchIdException
    {
        final String id = "abc-xyz";
        final BatchId bid = new BatchId(id);
        final String batchDirName = BatchNameProvider.getBatchDirectoryName(bid);
        System.out.println("testValidBatchId: " + batchDirName);
        final String batchId = new BatchInProgressDirectoryName(batchDirName).getBatchId();
        assertEquals(bid.getValue(), batchId);
    }

    @Test
    public void testValidBatchIdLikeServiceId() throws InvalidBatchIdException
    {
        final String id = ServiceIdentifier.getServiceId();
        final BatchId bid = new BatchId(id);
        final String batchDirName = BatchNameProvider.getBatchDirectoryName(bid);
        System.out.println("testValidBatchIdLikeServiceId: " + batchDirName);
        final String batchId = new BatchInProgressDirectoryName(batchDirName).getBatchId();
        assertEquals(bid.getValue(), batchId);
    }

    @Test
    public void testValidLongerBatchId() throws InvalidBatchIdException
    {
        final String id = "pqr-abc-xyz";
        final BatchId bid = new BatchId(id);
        final String batchDirName = BatchNameProvider.getBatchDirectoryName(bid);
        System.out.println("testValidLongerBatchId: " + batchDirName);
        final String batchId = new BatchInProgressDirectoryName(batchDirName).getBatchId();
        assertEquals(bid.getValue(), batchId);
    }
}
