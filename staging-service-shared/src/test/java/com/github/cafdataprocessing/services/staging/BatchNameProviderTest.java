package com.github.cafdataprocessing.services.staging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchNameProvider;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;

public final class BatchNameProviderTest {

    @Test
    void testValidBatchId() {
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
    void testValidBatchIdLikeServiceId() {
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
    void testValidLongerBatchId() {
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
    void testInvalidBatchId() {
        assertThrows(
            InvalidBatchIdException.class,
            () -> new BatchId("abc.."),
            "Expected InvalidBatchIdException"
        );
     }
}
