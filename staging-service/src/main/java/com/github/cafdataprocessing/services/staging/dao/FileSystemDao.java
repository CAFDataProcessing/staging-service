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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.cafdataprocessing.services.staging.models.StagedFile;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;

public class FileSystemDao implements BatchDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDao.class);

    private static final String DOCUMENT_JSON_CONTENT = "application/document+json";

    private static final String CONTENT_FILES = "files";

    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmssSSS";

    private static final String INPROGRESS_FOLDER = "in_progress";

    private final String basePath;
    private final int subbatchSize;

    public FileSystemDao(final String basePath, final int subbatchSize) {
        this.basePath = basePath;
        this.subbatchSize = subbatchSize;
    }

    @Override
    public List<String> getFiles(
            @Size(min = 1, max = 256) @Valid final String startsWith,
            @Size(min = 1, max = 256) @Valid final String from,
            @Min(1) @Valid final Integer limit) throws IOException {
        LOGGER.debug("Fetching batches starting with : {}", startsWith);
        final File directory = new File(this.basePath);
        if (!directory.exists()) {
            LOGGER.debug("Base folder does not exist...");
            return new ArrayList<>();
        }
        //Retrieve the current list of batches in alphabetical order
        final Stream<Path> pathStream = Files.walk(Paths.get(this.basePath), FileVisitOption.FOLLOW_LINKS);
        final List<String> fileNames = pathStream.filter(p -> p.toFile().isDirectory() && p.toFile().getName().startsWith(startsWith))
                  .sorted().map(p -> p.getFileName().toString()).collect(Collectors.toList());
        pathStream.close();
        //paginate
        int indexOfFromString = fileNames.indexOf(from);
        if(indexOfFromString == -1)
        {
            //TODO: just return from the beginning? or throw an error?
            indexOfFromString = 0;
        }
        int fetchLimit = limit;
        if(fileNames.size() < limit)
        {
            //Not enough items in list, so return as many as there are
            fetchLimit  = fileNames.size();
        }
        final List<String> pageFileList = fileNames.subList(indexOfFromString, fetchLimit);
        LOGGER.debug("Returning batch list : {}", pageFileList);
        return pageFileList;
    }

    @Override
    public void deleteFiles(@Size(min = 1) final String batchId) throws IOException {
        //get the batch folder
        final String batchFolderName = this.basePath.concat("/").concat(batchId);
        final File batchFolder = new File(batchFolderName);
        if (!batchFolder.exists()) {
            LOGGER.warn("Batch folder does not exist: {}", batchFolderName);
        }
        else
        {
            //Delete the folder and all its contents
            FileUtils.deleteDirectory(batchFolder);
        }
    }

    private void saveContentFile(
            final InputStreamSupplier inputStreamSupplier,
            final File contentFile) throws IOException {
        final InputStream inStream = inputStreamSupplier.get();
        FileUtils.copyInputStreamToFile(inStream, contentFile);
        inStream.close();
    }

    @Override
    public List<String> saveFiles(@Size(min = 1) String batchId, Stream<StagedFile> parts) throws JsonParseException, IOException {
        final List<String> fileNames = new ArrayList<>();
        /*
        - It will create a temporary folder for the batch to store files while the entire batch is being processed
        - It will open a subbatch file with the following naming pattern "YYYYMMDD-HHMMSS-xxx-json.batch"
        (where xxx forces uniqueness - ideally it would be derived from a service instance id and the thread id).
        - It will write the metadata of each document family out into the subbatch file in minified json format.
        When a configurable subbatch size is reached the subbatch file will be closed and a new one will be opened.
        - The batch submitted by AJP may also contain loose files referenced from the document family metadata.
        Any referenced files should be submitted in the batch before they are referred to.
        The referenced files will be separately written out to a files subfolder.
        If there are any failures while processing the batch, the temporary folder will be deleted
        If the entire batch was processed without errors the temporary folder will be renamed and moved to the staging root folder
        If the batch folder existed under the staging root, it will be deleted before copying the new data
        */
        //Make a temporary folder with name like, timestamp+threadid+serviceid+batchID in a separate "in_progress" folder
        final String inProgressBatchFolderName = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT))
                                               .concat("-").concat("" + Thread.currentThread().getId())
                                               .concat("-").concat(ServiceIdentifier.getServiceId())
                                               .concat("-").concat(batchId);
        
        final String inProgressBatchFolderPath = this.basePath.concat("/")
                                                .concat(INPROGRESS_FOLDER).concat("/")
                                                .concat(inProgressBatchFolderName);
        final File inProgressBatchFolder = new File(inProgressBatchFolderPath);
        if (!inProgressBatchFolder.exists()) {
            LOGGER.debug("Creating in-progress folder for batch: {}", inProgressBatchFolderPath);
            inProgressBatchFolder.mkdir();
        }
        final Iterator<StagedFile> stagedFilesIterator = parts.iterator();
        final SubBatchWriter subBatchOutStream = new SubBatchWriter(inProgressBatchFolder, subbatchSize);
        while(stagedFilesIterator.hasNext())
        {
            final StagedFile p = stagedFilesIterator.next();
            final InputStreamSupplier inputStreamSupplier = p.getInputStreamSupplier();
            try {
                if(p.getContentType().equalsIgnoreCase(DOCUMENT_JSON_CONTENT))
                {
                    subBatchOutStream.writeDocumentFile(inputStreamSupplier);
                }
                else
                {
                    //write referenced files to a 'files' subfolder
                    final File contentFile = new File(inProgressBatchFolder.getAbsolutePath().concat("/").concat(CONTENT_FILES)
                            .concat("/").concat(p.getName()));
                    saveContentFile(inputStreamSupplier, contentFile);
                    LOGGER.trace("Wrote content file '{}'", contentFile);
                }
                fileNames.add(p.getName());
            } catch (IOException e) {
                LOGGER.error("Error saving batch, in-progress batch folder '{}' will be deleted ", inProgressBatchFolderPath, e);
                subBatchOutStream.closeSubBatchOutputStream();
                //Delete the in-progress folder for this batch
                if(inProgressBatchFolder.exists() && inProgressBatchFolder.isDirectory())
                {
                    //Deletes directory recursively
                    FileUtils.deleteDirectory(inProgressBatchFolder);
                }
                throw e;
            }
        }
        subBatchOutStream.closeSubBatchOutputStream();
        //Move in_progress batch folder under the base and rename it
        final String batchFolderName = this.basePath.concat("/").concat(batchId);
        final File batchFolder = new File(batchFolderName);
        if(batchFolder.exists() && batchFolder.isDirectory())
        {
            //Remove existing batch folder
            LOGGER.debug("Removing existing batch folder: {}", batchFolder);
            FileUtils.deleteDirectory(batchFolder);
        }
        LOGGER.debug("Copying contents of temporary in-progress batch folder '{}' to staging service base folder '{}'", inProgressBatchFolder, batchFolder);
        FileUtils.copyDirectory(inProgressBatchFolder, batchFolder, TrueFileFilter.INSTANCE);
        //Now delete the temporary in-progress folder for batch
        LOGGER.debug("Removing temporary in-progress batch folder: {}", inProgressBatchFolder);
        FileUtils.deleteDirectory(inProgressBatchFolder);

        LOGGER.debug("Staged batch here : {}", batchFolder);
        LOGGER.trace("--- Batch contents : " + FileUtils.listFilesAndDirs(batchFolder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE));
        return fileNames;
    }
}
