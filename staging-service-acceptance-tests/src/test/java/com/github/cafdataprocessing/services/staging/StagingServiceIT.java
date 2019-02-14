/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.github.cafdataprocessing.services.staging;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

public class StagingServiceIT {

    private static final String STAGING_SERVICE_URI = System.getenv("staging-service");
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

        //Cleanup
        clearFiles(contentFiles);
        clearFiles(documentFiles);
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
        try{
            stagingApi.deleteBatch(batchId);
            final StagingBatchList listResponse = stagingApi.getBatches("delTest", "delTest", 10);
            assertTrue("deleteBatchTest, 0 batchs listed", listResponse.getEntries().size() == 0);
        }
        catch (ApiException ex){
            fail("deleteBatchTest failed");
        }
    }

    private StagingBatchResponse stageMultiParts(final String[] contentFiles, final String[] documentFiles)
            throws IOException, ApiException
    {
        final List<MultiPart> uploadData = new ArrayList<>();
        for(final String file : contentFiles)
        {
           final File contentFile = new File(file);
           FileUtils.copyInputStreamToFile(StagingServiceIT.class.getResourceAsStream("/" + file), contentFile);
           uploadData.add(new MultiPartFile(contentFile));
        }
        for(final String file : documentFiles)
        {
           final File documentFile = new File(file);
           FileUtils.copyInputStreamToFile(StagingServiceIT.class.getResourceAsStream("/" + file), documentFile);
           uploadData.add(new MultiPartDocument(documentFile));
        }
        return stagingApi.addDocumentsToBatch("testBatch", uploadData.stream());
    }

    private StagingBatchResponse stageMultiPartStreams(final String batchId, final String[] contentFiles, final String[] documentFiles)
            throws IOException, ApiException
    {
        final List<MultiPart> uploadData = new ArrayList<>();
        for(final String file : contentFiles)
        {
           uploadData.add(new MultiPartContentResource(file, StagingServiceIT.class.getResource("/" + file)));
        }
        for(final String file : documentFiles)
        {
           uploadData.add(new MultiPartDocumentResource(file, StagingServiceIT.class.getResource("/" + file)));
        }
        try
        {
            return stagingApi.addDocumentsToBatch(batchId, uploadData.stream());
        }
        catch (final ApiException ex){
            fail("stageMultiPartStreams failed : " + ex.getMessage()
               + " response code : " + ex.getCode()
               + " response body : " + ex.getResponseBody());
            throw ex;
        }
    }

    private void clearFiles(final String[] fileNames) throws IOException
    {
        for(final String fileName : fileNames)
        {
            Files.deleteIfExists(Paths.get(fileName));
        }
    }

}
