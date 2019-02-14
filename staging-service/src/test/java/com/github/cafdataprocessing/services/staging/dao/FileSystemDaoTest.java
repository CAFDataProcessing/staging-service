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
package com.github.cafdataprocessing.services.staging.dao;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cafdataprocessing.services.staging.dao.FileSystemDao;
import com.github.cafdataprocessing.services.staging.models.StagedFile;

public class FileSystemDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDaoTest.class);

    @Ignore
    public void saveFilesTest() {
        final String batchId = "testBatch";
        final Stream<StagedFile> parts;
    }

    @Test
    public void getFilesTest() throws IOException {
        final String startsWith = "test"; 
        final String from = "test";
        final Integer limit = 10;
        final String directoryName = "stagedFiles";
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(directoryName + "/testBatch"));
        Files.createDirectories(Paths.get(directoryName + "/abcBatch"));
        final File f1 = new File(directoryName + "/testBatch/test_Christmas_Carol1.txt");
        final File f2 = new File(directoryName + "/testBatch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> fileNames = fsDao.getFiles(startsWith, from, limit);
        assertTrue("getFilesTest : " + fileNames, fileNames.size() == 1);
        //Cleanup
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch/test_Christmas_Carol1.txt"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch/A_Christmas_Carol2.txt"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch"));
        Files.deleteIfExists(Paths.get(directoryName + "/abcBatch"));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    @Test
    public void getFilesInvalidFromTest() throws IOException {
        final String startsWith = "test"; 
        final String from = "best";
        final Integer limit = 10;
        final String directoryName = "stagedFiles";
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(directoryName + "/testBatch"));
        Files.createDirectories(Paths.get(directoryName + "/abcBatch"));
        final File f1 = new File(directoryName + "/testBatch/test_Christmas_Carol1.txt");
        final File f2 = new File(directoryName + "/testBatch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> fileNames = fsDao.getFiles(startsWith, from, limit);
        assertTrue("getFilesInvalidFromTest : " + fileNames, fileNames.size() == 1);
        //Cleanup
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch/test_Christmas_Carol1.txt"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch/A_Christmas_Carol2.txt"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch"));
        Files.deleteIfExists(Paths.get(directoryName + "/abcBatch"));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    @Test
    public void getFilesPaginateFromTest() throws IOException {
        final String startsWith = "test"; 
        final String from = "testBatch8";
        final Integer limit = 10;
        final String directoryName = "stagedFiles";
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        Files.createDirectories(Paths.get(directoryName + "/testBatch"));
        Files.createDirectories(Paths.get(directoryName + "/testBatch6"));
        Files.createDirectories(Paths.get(directoryName + "/testBatch7"));
        Files.createDirectories(Paths.get(directoryName + "/testBatch8"));
        Files.createDirectories(Paths.get(directoryName + "/testBatch9"));
        Files.createDirectories(Paths.get(directoryName + "/testBatch10"));
        Files.createDirectories(Paths.get(directoryName + "/abcBatch"));
        final File f1 = new File(directoryName + "/testBatch/test_Christmas_Carol1.txt");
        final File f2 = new File(directoryName + "/testBatch/A_Christmas_Carol2.txt");
        FileUtils.writeStringToFile(f1, "abc", "UTF8");
        FileUtils.writeStringToFile(f2, "def", "UTF8");
        FileSystemDao fsDao = new FileSystemDao(directoryName, 250);
        final List<String> fileNames = fsDao.getFiles(startsWith, from, limit);
        assertTrue("getFilesPaginateFromTest : " + fileNames, fileNames.size() == 2);
        //Cleanup
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch/test_Christmas_Carol1.txt"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch/A_Christmas_Carol2.txt"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch6"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch7"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch8"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch9"));
        Files.deleteIfExists(Paths.get(directoryName + "/testBatch10"));
        Files.deleteIfExists(Paths.get(directoryName + "/abcBatch"));
        Files.deleteIfExists(Paths.get(directoryName));
    }

    @Ignore
    public void deleteFilesTest() {
        final String batchId = "testBatch";
    }

}
