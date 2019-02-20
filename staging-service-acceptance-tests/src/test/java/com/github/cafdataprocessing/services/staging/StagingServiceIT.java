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
import com.github.cafdataprocessing.services.staging.client.MultiPartContentResource;
import com.github.cafdataprocessing.services.staging.client.MultiPartDocument;
import com.github.cafdataprocessing.services.staging.client.MultiPartDocumentResource;
import com.github.cafdataprocessing.services.staging.client.MultiPartFile;
import com.github.cafdataprocessing.services.staging.client.StagingApi;
import com.github.cafdataprocessing.services.staging.client.StagingBatchList;
import com.github.cafdataprocessing.services.staging.client.StagingBatchResponse;

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
    public void addDocumentsToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json"};
        final StagingBatchResponse response = stageMultiParts(contentFiles, documentFiles);
        assertTrue("addDocumentsToBatchTest, 3 files uploaded", response.getEntries().size() == 3);
    }

    @Test
    public void addMultipleDocumentsToBatchTest() throws Exception {
        final String batchId = "testBatch";
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json", "batch2.json", "batch3.json", "batch4.json", "batch5.json", "batch6.json"};
        final StagingBatchResponse response = stageMultiPartStreams(batchId, contentFiles, documentFiles);
        assertTrue("addMultipleDocumentsToBatchTest, 8 files uploaded", response.getEntries().size() == 8);
    }

    @Test
    public void addEmptyDocumentToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{"empty.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{};
        final StagingBatchResponse response = stageMultiParts(contentFiles, documentFiles);
        assertEquals(2, response.getEntries().size());
    }

    @Test
    public void addEmptyJSONToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{};
        final String[] documentFiles = new String[]{"empty.json"};
        final StagingBatchResponse response = stageMultiParts(contentFiles, documentFiles);
        assertEquals(1, response.getEntries().size());
    }

    @Test
    public void addInvalidJSONToBatchTest() throws Exception {
        final String[] contentFiles = new String[]{};
        final String[] documentFiles = new String[]{"not-json.json"};
        try{
            final StagingBatchResponse response = stageMultiParts(contentFiles, documentFiles);
            fail("Expected ApiException");
        }
        catch(ApiException ex){
            assertEquals(400, ex.getCode());
        }
    }

    @Test
    public void getBatchesTest() throws Exception {
        final String batchId = "testBatch";
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json",};
        final StagingBatchResponse response = stageMultiPartStreams(batchId, contentFiles, documentFiles);
        assertTrue("addDocumentsToBatchTest, 3 files uploaded", response.getEntries().size() == 3);

        final StagingBatchList listResponse = stagingApi.getBatches("test", "test", 10);
        assertTrue("getBatchesTest, 1 batch listed", listResponse.getEntries().size() == 1);
    }

    @Test
    public void deleteBatchTest() throws Exception {
        final String batchId = "delTestBatch";
        final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
        final String[] documentFiles = new String[]{"batch1.json", "batch2.json", "batch3.json", "batch4.json", "batch5.json", "batch6.json"};
        final StagingBatchResponse response = stageMultiPartStreams(batchId, contentFiles, documentFiles);
        assertTrue("addMultipleDocumentsToBatchTest, 8 files uploaded", response.getEntries().size() == 8);
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

    private StagingBatchResponse stageMultiParts(final String[] contentFiles, final String[] documentFiles)
            throws IOException, ApiException {
        final List<MultiPart> uploadData = new ArrayList<>();
        for (final String file : contentFiles) {
            final File contentFile = Paths.get(Files.createTempDirectory("batchBase").toString(), file).toFile();
            contentFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(StagingServiceIT.class.getResourceAsStream("/" + file), contentFile);
            uploadData.add(new MultiPartFile(contentFile));
        }
        for (final String file : documentFiles) {
            final File documentFile = new File(file);
            documentFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(StagingServiceIT.class.getResourceAsStream("/" + file), documentFile);
            uploadData.add(new MultiPartDocument(documentFile));
        }
        return stagingApi.addDocumentsToBatch("testBatch", uploadData.stream());
    }

    private StagingBatchResponse stageMultiPartStreams(final String batchId, final String[] contentFiles, final String[] documentFiles)
            throws IOException, ApiException {
        final List<MultiPart> uploadData = new ArrayList<>();
        for (final String file : contentFiles) {
            uploadData.add(new MultiPartContentResource(file, StagingServiceIT.class.getResource("/" + file)));
        }
        for (final String file : documentFiles) {
            uploadData.add(new MultiPartDocumentResource(file, StagingServiceIT.class.getResource("/" + file)));
        }
        try {
            return stagingApi.addDocumentsToBatch(batchId, uploadData.stream());
        } catch (final ApiException ex) {
            fail("stageMultiPartStreams failed : " + ex.getMessage()
                    + " response code : " + ex.getCode()
                    + " response body : " + ex.getResponseBody());
            throw ex;
        }
    }
}
