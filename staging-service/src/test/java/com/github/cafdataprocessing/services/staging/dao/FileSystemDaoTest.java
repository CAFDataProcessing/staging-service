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
package com.github.cafdataprocessing.services.staging.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchPathProvider;
import com.github.cafdataprocessing.services.staging.dao.filesystem.FileSystemDao;
import java.io.FileInputStream;
import org.apache.commons.io.FilenameUtils;

public class FileSystemDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDaoTest.class);
    private static final String BATCH_BASE_FOLDER = "batchBase";
    private static String TEST_TENANT_ID = "12345";
    private TenantId tenantId;
    private String baseDirName;
    private String storageDirName;
    final int fieldValueSizeThreshold = 8192; // 8KB

    @Before
    public void setUp() throws Exception {
        tenantId = new TenantId(TEST_TENANT_ID);
        baseDirName = getTempBaseBatchDir();
    }

    @After
    public void tearDown() throws IOException
    {
        final File baseDir = new File(baseDirName);
        if(baseDir.exists()){
            FileUtils.deleteDirectory(baseDir);
        }
    }

    @Test
    public void saveFilesTest() throws Exception {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("jsonDocument.json");
        when(f1.isFormField()).thenReturn(true);
        when(f1.openStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        FileItemStream f2 = mock(FileItemStream.class);
        when(f2.getContentType()).thenReturn("application/text");
        when(f2.getFieldName()).thenReturn("hello.txt");
        when(f2.isFormField()).thenReturn(true);
        when(f2.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, false);
        when(fileItemIterator.next()).thenReturn(f2, f1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
        assertEquals(2, files.size());
        assertTrue(files.contains(f1.getFieldName()));
        files.remove(f1.getFieldName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).equals("txt"));
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }
    
    @Test
    public void saveFilesWindowsPathTest() throws Exception {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("jsonDocument.json");
        when(f1.isFormField()).thenReturn(true);
        when(f1.openStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        FileItemStream f2 = mock(FileItemStream.class);
        when(f2.getContentType()).thenReturn("application/text");
        when(f2.getFieldName()).thenReturn("C:\\test\\hello.pdf");
        when(f2.isFormField()).thenReturn(true);
        when(f2.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, false);
        when(fileItemIterator.next()).thenReturn(f2, f1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
        assertEquals(2, files.size());
        assertTrue(files.contains(f1.getFieldName()));
        files.remove(f1.getFieldName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).equals("pdf"));
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }
    
    @Test
    public void saveFilesLinuxPathTest() throws Exception {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("jsonDocument.json");
        when(f1.isFormField()).thenReturn(true);
        when(f1.openStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        FileItemStream f2 = mock(FileItemStream.class);
        when(f2.getContentType()).thenReturn("application/text");
        when(f2.getFieldName()).thenReturn("/mnt/c/test/hello");
        when(f2.isFormField()).thenReturn(true);
        when(f2.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, false);
        when(fileItemIterator.next()).thenReturn(f2, f1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
        assertEquals(2, files.size());
        assertTrue(files.contains(f1.getFieldName()));
        files.remove(f1.getFieldName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).isEmpty());
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }
    
    @Test
    public void saveInvalidLinuxPathTest() throws Exception {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("jsonDocument.json");
        when(f1.isFormField()).thenReturn(true);
        when(f1.openStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        FileItemStream f2 = mock(FileItemStream.class);
        when(f2.getContentType()).thenReturn("application/text");
        when(f2.getFieldName()).thenReturn("../../mnt/c/test/hello");
        when(f2.isFormField()).thenReturn(true);
        when(f2.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, false);
        when(fileItemIterator.next()).thenReturn(f2, f1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
        assertEquals(2, files.size());
        assertTrue(files.contains(f1.getFieldName()));
        files.remove(f1.getFieldName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).isEmpty());
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }
    
    @Test
    public void saveFilesWrongOrderNegativeTest() throws Exception
    {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("jsonDocument.json");
        when(f1.isFormField()).thenReturn(true);
        when(f1.openStream()).thenReturn(new FileInputStream(Paths.get("src", "test", "resources", "batch1.json").toFile()));

        FileItemStream f2 = mock(FileItemStream.class);
        when(f2.getContentType()).thenReturn("application/text");
        when(f2.getFieldName()).thenReturn("A_Christmas_Carol1.txt");
        when(f2.isFormField()).thenReturn(true);
        when(f2.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, false);
        when(fileItemIterator.next()).thenReturn(f1, f2);

        try {
            fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
            fail("An exception should have been thrown");
        } catch (InvalidBatchException ex) {
            assertTrue(ex.getMessage().contains("Binary files referenced in the JSON documents must be uploaded before the JSON "
                + "documents. Check file A_Christmas_Carol1.txt"));
        }
    }
    
    @Test
    public void saveFilesWrongOrderMixedNegativeTest() throws Exception
    {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream jsonDocOk = mock(FileItemStream.class);
        when(jsonDocOk.getContentType()).thenReturn("application/document+json");
        when(jsonDocOk.getFieldName()).thenReturn("jsonDocument.json");
        when(jsonDocOk.isFormField()).thenReturn(true);
        when(jsonDocOk.openStream()).thenReturn(new FileInputStream(Paths.get("src", "test", "resources", "batch1.json").toFile()));

        FileItemStream localRefOneOk = mock(FileItemStream.class);
        when(localRefOneOk.getContentType()).thenReturn("application/text");
        when(localRefOneOk.getFieldName()).thenReturn("A_Christmas_Carol1.txt");
        when(localRefOneOk.isFormField()).thenReturn(true);
        when(localRefOneOk.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));
        
        FileItemStream localRefTwoOk = mock(FileItemStream.class);
        when(localRefTwoOk.getContentType()).thenReturn("application/text");
        when(localRefTwoOk.getFieldName()).thenReturn("A_Christmas_Carol2.txt");
        when(localRefTwoOk.isFormField()).thenReturn(true);
        when(localRefTwoOk.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));
        
        FileItemStream jsonDocNotOk = mock(FileItemStream.class);
        when(jsonDocNotOk.getContentType()).thenReturn("application/document+json");
        when(jsonDocNotOk.getFieldName()).thenReturn("jsonDocument.json");
        when(jsonDocNotOk.isFormField()).thenReturn(true);
        when(jsonDocNotOk.openStream()).thenReturn(new FileInputStream(
            Paths.get("src", "test", "resources", "batch1MissingLocalRef.json").toFile()));
        
        FileItemStream localRefThreeTooLate = mock(FileItemStream.class);
        when(localRefThreeTooLate.getContentType()).thenReturn("application/text");
        when(localRefThreeTooLate.getFieldName()).thenReturn("hello-hello.txt");
        when(localRefThreeTooLate.isFormField()).thenReturn(true);
        when(localRefThreeTooLate.openStream()).thenReturn(new ByteArrayInputStream("Hello hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, true, true, true, false);
        when(fileItemIterator.next()).thenReturn(localRefOneOk, localRefTwoOk, jsonDocOk, jsonDocNotOk, localRefThreeTooLate);

        try {
            fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
            fail("An exception should have been thrown");
        } catch (InvalidBatchException ex) {
            assertTrue(ex.getMessage().contains("Binary files referenced in the JSON documents must be uploaded before the "
                + "JSON documents. Check file A_Christmas_Carol3.txt"));
        }
    }
    
    @Test
    public void missingLocalRefFileTest() throws Exception {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());
        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("batch1.json");
        when(f1.isFormField()).thenReturn(true);
        when(f1.openStream()).thenReturn(new FileInputStream(Paths.get("src", "test", "resources", "batch1.json").toString()));

        FileItemStream f2 = mock(FileItemStream.class);
        when(f2.getContentType()).thenReturn("application/text");
        when(f2.getFieldName()).thenReturn("hello.txt");
        when(f2.isFormField()).thenReturn(true);
        when(f2.openStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, true, false);
        when(fileItemIterator.next()).thenReturn(f1, f2);
        
        try {
            fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
            fail("The exception has not been thrown!");
        } catch (InvalidBatchException ex) {
            assertTrue(ex.getMessage().contains("Binary files referenced in the JSON documents must be uploaded before the JSON "
                + "documents. Check file A_Christmas_Carol1.txt"));
        }
    }

    @Test
    public void saveInvalidJsonTest() throws Exception {
        final FileSystemDao fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        FileItemStream f1 = mock(FileItemStream.class);
        when(f1.getContentType()).thenReturn("application/document+json");
        when(f1.getFieldName()).thenReturn("jsonDocument.json");
        when(f1.isFormField()).thenReturn(true);
        // Invalid json
        when(f1.openStream()).thenReturn(new ByteArrayInputStream("{\"abc\": \"lll\",}".getBytes()));

        FileItemIterator fileItemIterator = mock(FileItemIterator.class);
        when(fileItemIterator.hasNext()).thenReturn(true, false);
        when(fileItemIterator.next()).thenReturn(f1);

        try
        {
            fileSystemDao.saveFiles(tenantId, batchId, fileItemIterator);
            fail("Incorrectly uploaded invalid batch");
        }
        catch(final InvalidBatchException e)
        {
            assertTrue("Expected InvalidBatchException thrown", true);
        }
    }

    @Test(expected = InvalidBatchIdException.class)
    public void putFilesInvalidBatchIdTest() throws Exception {
        new BatchId("../../MyBadBatchId");
    }

    @Test
    public void getFilesTest() throws Exception {
        final String startsWith = "test"; 
        final String from = "test";
        final Integer limit = 10;

        final String completedDirectoryName = getCompletedBatchDir(tenantId,baseDirName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/test-batch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);

        final List<String> fileNames = fsDao.getBatches(tenantId, startsWith, new BatchId(from), limit);
        assertTrue("getFilesTest : " + fileNames, fileNames.size() == 1);
    }

    @Test
    public void getFilesInvalidFromTest() throws Exception {
        final String startsWith = "test"; 
        final String from = "best";
        final Integer limit = 10;
        final String completedDirectoryName = getCompletedBatchDir(tenantId, baseDirName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/test-batch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final List<String> fileNames = fsDao.getBatches(tenantId, startsWith, new BatchId(from), limit);
        assertTrue("getFilesInvalidFromTest : " + fileNames, fileNames.size() == 1);
    }

    @Test
    public void getFilesPaginateFromTest() throws Exception {
        final String startsWith = "test"; 
        final String from = "test-batch8";
        final Integer limit = 10;
        final String completedDirectoryName = getCompletedBatchDir(tenantId, baseDirName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch6"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch7"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch8"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch9"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch10"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/test-batch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final List<String> fileNames = fsDao.getBatches(tenantId, startsWith, new BatchId(from), limit);
        assertTrue("getFilesPaginateFromTest : " + fileNames, fileNames.size() == 2);
    }

    @Test
    public void getFilesPaginate() throws Exception {
        final String completedDirectoryName = getCompletedBatchDir(tenantId, baseDirName);
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch/files"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch6"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch6/files"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch7"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch8"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch9"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch10"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/files/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/test-batch/files/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final List<String> fileNames = fsDao.getBatches(tenantId, null, null, 25);
        assertTrue("getFilesPaginate : " + fileNames, fileNames.size() == 7);
    }

    @Test
    public void deleteFilesTest() throws Exception {
        final BatchId batchId = new BatchId("test-batch");

        final String completedDirectoryName = getCompletedBatchDir(tenantId, baseDirName);
        Files.createDirectories(Paths.get(completedDirectoryName , "/test-batch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/test_Christmas_Carol1.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");

        FileSystemDao fsDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold);
        final List<String> batches = fsDao.getBatches(tenantId, null, null, null);
        assertEquals(1, batches.size());
        assertTrue(batches.contains(batchId.getValue()));

        fsDao.deleteBatch(tenantId, batchId);

        final List<String> batchesAfterDelete = fsDao.getBatches(tenantId, null, null, null);
        assertEquals(0, batchesAfterDelete.size());
    }

    private String getTempBaseBatchDir() throws Exception {
        return Files.createTempDirectory(BATCH_BASE_FOLDER).toString();
    }

    private String getCompletedBatchDir(final TenantId tenantId, final String baseDir) throws Exception {
        return Files.createDirectories(Paths.get(baseDir , tenantId.getValue(), BatchPathProvider.COMPLETED_FOLDER)).toString();
    }
    
    private boolean isUUIDvalid(final String uuid)
    {
        LOGGER.debug("Validating UUID {}", uuid);
        return uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

}
