/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.github.cafdataprocessing.services.staging.client.ApiClient;
import com.github.cafdataprocessing.services.staging.client.ApiException;
import com.github.cafdataprocessing.services.staging.client.MultiPart;
import com.github.cafdataprocessing.services.staging.client.MultiPartContent;
import com.github.cafdataprocessing.services.staging.client.MultiPartDocument;
import com.github.cafdataprocessing.services.staging.client.StagingApi;
import com.github.cafdataprocessing.services.staging.client.StagingBatchList;

import static org.junit.Assert.*;

public class StagingServiceIT {

    private static final String STAGING_SERVICE_URI = System.getenv("staging-service");
//        private static final String STAGING_SERVICE_URI = "http://localhost:8080";
    private final StagingApi stagingApi;

    public StagingServiceIT() {
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(STAGING_SERVICE_URI);
        stagingApi = new StagingApi();
        stagingApi.setApiClient(apiClient);
    }

    @Test
    public void uploadDocumentsToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json"};
        final String batchId = "testBatch1";
        stageMultiParts(batchId, contentFiles, documentFiles);
        final StagingBatchList response = stagingApi.getBatches(batchId, batchId, 10);
        assertTrue("uploadDocumentsToBatchTest, 1 batch uploaded", response.getEntries().size() == 1);
    }

    @Test
    public void uploadMultipleDocumentsToBatchTest() throws Exception {
        final String batchId = "testBatchMultiple";
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json", "batch2.json", "batch3.json", "batch4.json", "batch5.json", "batch6.json"};
        stageMultiPartStreams(batchId, contentFiles, documentFiles);
        final StagingBatchList response = stagingApi.getBatches(batchId, batchId, 10);
        assertTrue("uploadMultipleDocumentsToBatchTest, 1 batch uploaded", response.getEntries().size() == 1);
    }

    @Test
    public void uploadEmptyDocumentToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{"empty.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{};
        final String batchId = "testBatchEmptyDoc";
        stageMultiParts(batchId, contentFiles, documentFiles);
        final StagingBatchList response = stagingApi.getBatches(batchId, batchId, 10);
        assertEquals(1, response.getEntries().size());
    }

    @Test
    public void uploadEmptyJSONToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{};
        final String[] documentFiles = new String[]{"empty.json"};
        final String batchId = "testBatchEmptyJson";
        stageMultiParts(batchId, contentFiles, documentFiles);
        final StagingBatchList response = stagingApi.getBatches(batchId, batchId, 10);
        assertEquals(1, response.getEntries().size());
    }

    @Test
    public void uploadInvalidJSONToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{};
        final String[] documentFiles = new String[]{"not-json.json"};
        try{
            stageMultiParts("testBatchInvalidJSON", contentFiles, documentFiles);
            fail("Expected ApiException");
        }
        catch(ApiException ex){
            assertEquals(400, ex.getCode());
        }
    }

    @Test
    public void getBatchesTest() throws Exception {
        final String batchId = "abcTestBatch";
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json",};
        stageMultiPartStreams(batchId, contentFiles, documentFiles);
        final StagingBatchList listResponse = stagingApi.getBatches("abc", "abc", 10);
        assertTrue("getBatchesTest, 1 batch listed", listResponse.getEntries().size() == 1);
    }

    @Test
    public void deleteBatchTest() throws Exception {
        final String batchId = "delTestBatch";
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json", "batch2.json", "batch3.json", "batch4.json", "batch5.json", "batch6.json"};
        stageMultiPartStreams(batchId, contentFiles, documentFiles);

        try {
            stagingApi.deleteBatch(batchId);
            final StagingBatchList listResponse = stagingApi.getBatches("delTest", "delTest", 10);
            assertTrue("deleteBatchTest, 0 batchs listed", listResponse.getEntries().size() == 0);
        } catch (ApiException ex) {
            fail("deleteBatchTest failed");
        }
    }

    @Test
    public void deleteNonExistingBatchTest() throws Exception {
        final String batchId = "delNonExistingTestBatch";
        try {
            stagingApi.deleteBatch(batchId);
            fail("Expected ApiException");
        } catch (ApiException ex) {
            assertEquals(404, ex.getCode());
        }
    }

    private void stageMultiParts(final String batchId, final String[] contentFiles, final String[] documentFiles)
            throws IOException, ApiException {
        final List<MultiPart> uploadData = new ArrayList<>();
        for (final String file : contentFiles) {
            final File contentFile = Paths.get(Files.createTempDirectory("batchBase").toString(), file).toFile();
            contentFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(StagingServiceIT.class.getResourceAsStream("/" + file), contentFile);
            uploadData.add(new MultiPartContent(contentFile));
        }
        for (final String file : documentFiles) {
            final File documentFile = new File(file);
            documentFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(StagingServiceIT.class.getResourceAsStream("/" + file), documentFile);
            uploadData.add(new MultiPartDocument(documentFile));
        }
        stagingApi.createOrReplaceBatch(batchId, uploadData.stream());
    }

    private void stageMultiPartStreams(final String batchId, final String[] contentFiles, final String[] documentFiles)
            throws IOException, ApiException {
        final List<MultiPart> uploadData = new ArrayList<>();
        for (final String file : contentFiles) {
            uploadData.add(new MultiPartContent(file, StagingServiceIT.class.getResource("/" + file)));
        }
        for (final String file : documentFiles) {
            uploadData.add(new MultiPartDocument(StagingServiceIT.class.getResource("/" + file)));
        }
        try {
            stagingApi.createOrReplaceBatch(batchId, uploadData.stream());
        } catch (final ApiException ex) {
            fail("stageMultiPartStreams failed : " + ex.getMessage()
                    + " response code : " + ex.getCode()
                    + " response body : " + ex.getResponseBody());
            throw ex;
        }
    }
}
