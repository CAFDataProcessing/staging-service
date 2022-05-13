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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchPathProvider;
import com.github.cafdataprocessing.services.staging.dao.filesystem.FileSystemDao;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.http.Part;
import org.apache.commons.io.FilenameUtils;

public class FileSystemDaoTest
{

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDaoTest.class);
    private static final String BATCH_BASE_FOLDER = "batchBase";
    private static String TEST_TENANT_ID = "12345";
    private TenantId tenantId;
    private String baseDirName;
    private String storageDirName = "/etc/store/batches";
    private FileSystemDao fileSystemDao;
    final int fieldValueSizeThreshold = 8192; // 8KB

    @Before
    public void setUp() throws Exception
    {
        tenantId = new TenantId(TEST_TENANT_ID);
        baseDirName = getTempBaseBatchDir();
        this.fileSystemDao = new FileSystemDao(baseDirName, 250, storageDirName, fieldValueSizeThreshold, 36000000, true);
    }

    @After
    public void tearDown() throws IOException
    {
        final File baseDir = new File(baseDirName);
        if (baseDir.exists()) {
            FileUtils.deleteDirectory(baseDir);
        }
    }

    @Test
    public void saveFilesTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("jsonDocument.json");
        when(part1.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        Part part2 = mock(Part.class);
        when(part2.getContentType()).thenReturn("application/text");
        when(part2.getName()).thenReturn("hello.txt");
        when(part2.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part2);
        parts.add(part1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, parts);
        assertEquals(2, files.size());
        assertTrue(files.contains(part1.getName()));
        files.remove(part1.getName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).equals("txt"));
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }

    @Test
    public void saveFilesWindowsPathTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("jsonDocument.json");
        when(part1.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        Part part2 = mock(Part.class);
        when(part2.getContentType()).thenReturn("application/text");
        when(part2.getName()).thenReturn("C:\\test\\hello.pdf");
        when(part2.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part2);
        parts.add(part1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, parts);
        assertEquals(2, files.size());
        assertTrue(files.contains(part1.getName()));
        files.remove(part1.getName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).equals("pdf"));
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }

    @Test
    public void saveFilesLinuxPathTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("jsonDocument.json");
        when(part1.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        Part part2 = mock(Part.class);
        when(part2.getContentType()).thenReturn("application/text");
        when(part2.getName()).thenReturn("/mnt/c/test/hello");
        when(part2.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part2);
        parts.add(part1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, parts);
        assertEquals(2, files.size());
        assertTrue(files.contains(part1.getName()));
        files.remove(part1.getName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).isEmpty());
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }

    @Test
    public void saveInvalidLinuxPathTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("jsonDocument.json");
        when(part1.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        Part part2 = mock(Part.class);
        when(part2.getContentType()).thenReturn("application/text");
        when(part2.getName()).thenReturn("../../mnt/c/test/hello");
        when(part2.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part2);
        parts.add(part1);

        final List<String> files = fileSystemDao.saveFiles(tenantId, batchId, parts);
        assertEquals(2, files.size());
        assertTrue(files.contains(part1.getName()));
        files.remove(part1.getName());
        assertTrue(files.size() == 1);
        assertTrue(FilenameUtils.getExtension(files.get(0)).isEmpty());
        assertTrue(isUUIDvalid(FilenameUtils.getBaseName(files.get(0))));
    }

    @Test
    public void saveFilesWrongOrderNegativeTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("jsonDocument.json");
        when(part1.getInputStream()).thenReturn(new FileInputStream(Paths.get("src", "test", "resources", "batch1.json").toFile()));

        Part part2 = mock(Part.class);
        when(part2.getContentType()).thenReturn("application/text");
        when(part2.getName()).thenReturn("A_Christmas_Carol1.txt");
        when(part2.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part1);
        parts.add(part2);

        try {
            fileSystemDao.saveFiles(tenantId, batchId, parts);
            fail("An exception should have been thrown");
        } catch (InvalidBatchException ex) {
            assertTrue(ex.getMessage().contains("Binary files referenced in the JSON documents must be uploaded before the JSON "
                + "documents. Check file A_Christmas_Carol1.txt"));
        }
    }

    @Test
    public void saveFilesWrongOrderMixedNegativeTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part jsonDocOk = mock(Part.class);
        when(jsonDocOk.getContentType()).thenReturn("application/document+json");
        when(jsonDocOk.getName()).thenReturn("jsonDocument.json");
        when(jsonDocOk.getInputStream()).thenReturn(new FileInputStream(Paths.get("src", "test", "resources", "batch1.json").toFile()));

        Part localRefOneOk = mock(Part.class);
        when(localRefOneOk.getContentType()).thenReturn("application/text");
        when(localRefOneOk.getName()).thenReturn("A_Christmas_Carol1.txt");
        when(localRefOneOk.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        Part localRefTwoOk = mock(Part.class);
        when(localRefTwoOk.getContentType()).thenReturn("application/text");
        when(localRefTwoOk.getName()).thenReturn("A_Christmas_Carol2.txt");
        when(localRefTwoOk.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        Part jsonDocNotOk = mock(Part.class);
        when(jsonDocNotOk.getContentType()).thenReturn("application/document+json");
        when(jsonDocNotOk.getName()).thenReturn("jsonDocument.json");
        when(jsonDocNotOk.getInputStream()).thenReturn(new FileInputStream(
            Paths.get("src", "test", "resources", "batch1MissingLocalRef.json").toFile()));

        Part localRefThreeTooLate = mock(Part.class);
        when(localRefThreeTooLate.getContentType()).thenReturn("application/text");
        when(localRefThreeTooLate.getName()).thenReturn("hello-hello.txt");
        when(localRefThreeTooLate.getInputStream()).thenReturn(new ByteArrayInputStream("Hello hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(localRefOneOk);
        parts.add(localRefTwoOk);
        parts.add(jsonDocOk);
        parts.add(jsonDocNotOk);
        parts.add(localRefThreeTooLate);

        try {
            fileSystemDao.saveFiles(tenantId, batchId, parts);
            fail("An exception should have been thrown");
        } catch (InvalidBatchException ex) {
            assertTrue(ex.getMessage().contains("Binary files referenced in the JSON documents must be uploaded before the "
                + "JSON documents. Check file A_Christmas_Carol3.txt"));
        }
    }

    @Test
    public void missingLocalRefFileTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("batch1.json");
        when(part1.getInputStream()).thenReturn(new FileInputStream(Paths.get("src", "test", "resources", "batch1.json").toString()));

        Part part2 = mock(Part.class);
        when(part2.getContentType()).thenReturn("application/text");
        when(part2.getName()).thenReturn("hello.txt");
        when(part2.getInputStream()).thenReturn(new ByteArrayInputStream("Hello".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part1);
        parts.add(part2);

        try {
            fileSystemDao.saveFiles(tenantId, batchId, parts);
            fail("The exception has not been thrown!");
        } catch (InvalidBatchException ex) {
            assertTrue(ex.getMessage().contains("Binary files referenced in the JSON documents must be uploaded before the JSON "
                + "documents. Check file A_Christmas_Carol1.txt"));
        }
    }

    @Test
    public void saveInvalidJsonTest() throws Exception
    {
        final BatchId batchId = new BatchId(UUID.randomUUID().toString());

        Part part1 = mock(Part.class);
        when(part1.getContentType()).thenReturn("application/document+json");
        when(part1.getName()).thenReturn("jsonDocument.json");
        // Invalid json
        when(part1.getInputStream()).thenReturn(new ByteArrayInputStream("{\"abc\": \"lll\",}".getBytes()));

        final Collection<Part> parts = new ArrayList<>();
        parts.add(part1);

        try {
            fileSystemDao.saveFiles(tenantId, batchId, parts);
            fail("Incorrectly uploaded invalid batch");
        } catch (final InvalidBatchException e) {
            assertTrue("Expected InvalidBatchException thrown", true);
        }
    }

    @Test(expected = InvalidBatchIdException.class)
    public void putFilesInvalidBatchIdTest() throws Exception
    {
        new BatchId("../../MyBadBatchId");
    }

    @Test
    public void getFilesTest() throws Exception
    {
        final String startsWith = "test";
        final String from = "test";
        final Integer limit = 10;

        final String completedDirectoryName = getCompletedBatchDir(tenantId, baseDirName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/test-batch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/test-batch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");

        final List<String> fileNames = fileSystemDao.getBatches(tenantId, startsWith, new BatchId(from), limit);
        assertTrue("getFilesTest : " + fileNames, fileNames.size() == 1);
    }

    @Test
    public void getFilesInvalidFromTest() throws Exception
    {
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
        final List<String> fileNames = fileSystemDao.getBatches(tenantId, startsWith, new BatchId(from), limit);
        assertTrue("getFilesInvalidFromTest : " + fileNames, fileNames.size() == 1);
    }

    @Test
    public void getFilesPaginateFromTest() throws Exception
    {
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
        final List<String> fileNames = fileSystemDao.getBatches(tenantId, startsWith, new BatchId(from), limit);
        assertTrue("getFilesPaginateFromTest : " + fileNames, fileNames.size() == 2);
    }

    @Test
    public void getFilesPaginate() throws Exception
    {
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
        final List<String> fileNames = fileSystemDao.getBatches(tenantId, null, null, 25);
        assertTrue("getFilesPaginate : " + fileNames, fileNames.size() == 7);
    }

    @Test
    public void deleteFilesTest() throws Exception
    {
        final BatchId batchId = new BatchId("test-batch");

        final String completedDirectoryName = getCompletedBatchDir(tenantId, baseDirName);
        Files.createDirectories(Paths.get(completedDirectoryName, "/test-batch"));
        final File f1 = new File(completedDirectoryName + "/test-batch/test_Christmas_Carol1.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");

        final List<String> batches = fileSystemDao.getBatches(tenantId, null, null, null);
        assertEquals(1, batches.size());
        assertTrue(batches.contains(batchId.getValue()));

        fileSystemDao.deleteBatch(tenantId, batchId);

        final List<String> batchesAfterDelete = fileSystemDao.getBatches(tenantId, null, null, null);
        assertEquals(0, batchesAfterDelete.size());
    }

    private String getTempBaseBatchDir() throws Exception
    {
        return Files.createTempDirectory(BATCH_BASE_FOLDER).toString();
    }

    private String getCompletedBatchDir(final TenantId tenantId, final String baseDir) throws Exception
    {
        return Files.createDirectories(Paths.get(baseDir, tenantId.getValue(), BatchPathProvider.COMPLETED_FOLDER)).toString();
    }

    private boolean isUUIDvalid(final String uuid)
    {
        LOGGER.debug("Validating UUID {}", uuid);
        return uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

}
