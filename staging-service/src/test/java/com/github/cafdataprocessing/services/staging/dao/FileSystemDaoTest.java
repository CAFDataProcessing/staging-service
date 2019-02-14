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
