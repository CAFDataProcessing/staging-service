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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchIdException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchPathProvider;
import com.github.cafdataprocessing.services.staging.dao.filesystem.FileSystemDao;

public class FileSystemDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDaoTest.class);

    @Test
    public void saveFilesTest() throws Exception {
        final String directoryName = getTempBaseBatchDir();
        final FileSystemDao fileSystemDao = new FileSystemDao(directoryName, 250);
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
        when(fileItemIterator.next()).thenReturn(f1, f2);

        final List<String> files = fileSystemDao.saveFiles(batchId, fileItemIterator);
        assertEquals(2, files.size());
        assertTrue(files.contains(f1.getFieldName()));
        assertTrue(files.contains(f2.getFieldName()));

        //Cleanup
        FileUtils.deleteDirectory(new File(directoryName));
    }

    @Test(expected = InvalidBatchIdException.class)
    public void putFilesInvalidBatchIdTest() throws Exception {
        final BatchId batchId = new BatchId("../../MyBadBatchId");
    }

    @Test
    public void getFilesTest() throws Exception {
        final String startsWith = "test"; 
        final String from = "test";
        final Integer limit = 10;

        final String directoryName = getTempBaseBatchDir();
        final String completedDirectoryName = getCompletedBatchDir(directoryName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/testBatch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> fileNames = fsDao.getBatches(startsWith, new BatchId(from), limit);
        assertTrue("getFilesTest : " + fileNames, fileNames.size() == 1);
        //Cleanup
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch/A_Christmas_Carol2.txt"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/abcBatch"));
        Files.deleteIfExists(Paths.get(completedDirectoryName));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    @Test
    public void getFilesInvalidFromTest() throws Exception {
        final String startsWith = "test"; 
        final String from = "best";
        final Integer limit = 10;
        final String directoryName = getTempBaseBatchDir();
        final String completedDirectoryName = getCompletedBatchDir(directoryName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/testBatch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> fileNames = fsDao.getBatches(startsWith, new BatchId(from), limit);
        assertTrue("getFilesInvalidFromTest : " + fileNames, fileNames.size() == 1);
        //Cleanup
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch/A_Christmas_Carol2.txt"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/abcBatch"));
        Files.deleteIfExists(Paths.get(completedDirectoryName));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    @Test
    public void getFilesPaginateFromTest() throws Exception {
        final String startsWith = "test"; 
        final String from = "testBatch8";
        final Integer limit = 10;
        final String directoryName = getTempBaseBatchDir();
        final String completedDirectoryName = getCompletedBatchDir(directoryName);
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch6"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch7"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch8"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch9"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/testBatch10"));
        Files.createDirectories(Paths.get(completedDirectoryName + "/abcBatch"));
        final File f1 = new File(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt");
        final File f2 = new File(completedDirectoryName + "/testBatch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> fileNames = fsDao.getBatches(startsWith, new BatchId(from), limit);
        assertTrue("getFilesPaginateFromTest : " + fileNames, fileNames.size() == 2);
        //Cleanup
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch/A_Christmas_Carol2.txt"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch6"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch7"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch8"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch9"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/testBatch10"));
        Files.deleteIfExists(Paths.get(completedDirectoryName + "/abcBatch"));
        Files.deleteIfExists(Paths.get(completedDirectoryName));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    @Test
    public void deleteFilesTest() throws Exception {
        final BatchId batchId = new BatchId("testBatch");

        final String directoryName = getTempBaseBatchDir();
        final String completedDirectoryName = getCompletedBatchDir(directoryName);
        Files.createDirectories(Paths.get(completedDirectoryName , "/testBatch"));
        final File f1 = new File(completedDirectoryName + "/testBatch/test_Christmas_Carol1.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");

        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> batches = fsDao.getBatches(null, null, null);
        assertEquals(1, batches.size());
        assertTrue(batches.contains(batchId.getValue()));

        fsDao.deleteBatch(batchId);

        final List<String> batchesAfterDelete = fsDao.getBatches(null, null, null);
        assertEquals(0, batchesAfterDelete.size());
        //Cleanup
        Files.deleteIfExists(Paths.get(completedDirectoryName));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    private String getTempBaseBatchDir() throws Exception {
        return Files.createTempDirectory("batchBase").toString();
    }

    private String getCompletedBatchDir(final String baseDir) throws Exception {
        return Files.createDirectories(Paths.get(baseDir , BatchPathProvider.COMPLETED_FOLDER)).toString();
    }

}
