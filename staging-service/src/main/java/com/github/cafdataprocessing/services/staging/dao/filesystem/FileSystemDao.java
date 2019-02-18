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
package com.github.cafdataprocessing.services.staging.dao.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.exceptions.BatchNotFoundException;
import com.github.cafdataprocessing.services.staging.exceptions.IncompleteBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemDao implements BatchDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDao.class);

    private static final String DOCUMENT_JSON_CONTENT = "application/document+json";

    private static final String CONTENT_FILES = "files";

    private final BatchPathProvider batchPathProvider;
    private final int subbatchSize;

    public FileSystemDao(final String basePath, final int subbatchSize) {
        batchPathProvider = new BatchPathProvider(basePath);
        this.subbatchSize = subbatchSize;
    }

    @Override
    public List<String> getBatches(
            @Size(min = 1, max = 256) @Valid final String startsWith,
            @Size(min = 1, max = 256) @Valid final String from,
            @Min(1) @Valid final Integer limit) throws StagingException {

        LOGGER.debug("Fetching batches starting with : {}", startsWith);

        //Retrieve the current list of batches in alphabetical order
        try(final Stream<Path> pathStream =
                    Files.walk(batchPathProvider.getPathForBatches(), FileVisitOption.FOLLOW_LINKS)){

            Stream<String> batchDirectoryNames = pathStream.map(Path::toFile).
                    filter(File::isDirectory).map(File::getName);

            if(startsWith!=null){
                batchDirectoryNames = batchDirectoryNames.filter(f -> f.startsWith(startsWith));
            }

            if(from!=null){
                batchDirectoryNames = batchDirectoryNames.filter(f -> f.compareTo(from)>0);
            }

            if(limit!=null){
                batchDirectoryNames = batchDirectoryNames.limit(limit);
            }

            return batchDirectoryNames.sorted().collect(Collectors.toList());
        }
        catch(IOException ex){
            throw new StagingException(ex);
        }
    }

    @Override
    public void deleteFiles(@Size(min = 1) final String batchId) throws BatchNotFoundException, StagingException {
        final Path batchPath = batchPathProvider.getPathForBatch(batchId);
        if(!batchPath.toFile().exists()){
            throw new BatchNotFoundException(batchId);
        }

        try {
            FileUtils.deleteDirectory(batchPath.toFile());
        } catch (IOException ex) {
            throw new StagingException(ex);
        }
    }

    @Override
    public List<String> saveFiles(@Size(min = 1) String batchId, FileItemIterator fileItemIterator)
            throws StagingException, InvalidBatchException, IncompleteBatchException {

        final Path inProgressBatchFolderPath = batchPathProvider.getInProgressPathForBatch(batchId);
        final List<String> fileNames = new ArrayList<>();
        try(final SubBatchWriter subBatchWriter = new SubBatchWriter(inProgressBatchFolderPath.toFile(), subbatchSize)){
            while(true){

                final FileItemStream fileItemStream;
                try{
                    if (!fileItemIterator.hasNext()){
                        break;
                    }
                    fileItemStream = fileItemIterator.next();
                }
                catch (FileUploadException | IOException ex){
                    throw new IncompleteBatchException(ex);
                }

                if(fileItemStream.isFormField()){
                    LOGGER.error("{} is not a file", fileItemStream.getName());
                    throw new InvalidBatchException(String.format("FileItemStream [%s] is not a form field.", fileItemStream.getName()));
                }
                if(!fileItemStream.getFieldName().equals("uploadData")){
                    LOGGER.error("{} is not in 'uploadData'. Invalid field name : {}", fileItemStream.getName(),
                            fileItemStream.getFieldName());
                    throw new InvalidBatchException(String.format("Unexpected form field [%s].", fileItemStream.getFieldName()));
                }

                final String contentType = fileItemStream.getContentType();
                final String normalizedFilename = Paths.get(fileItemStream.getName()).toFile().getName();

                if(contentType.equalsIgnoreCase(DOCUMENT_JSON_CONTENT))
                {
                    subBatchWriter.writeDocumentFile(fileItemStream::openStream);
                }
                else
                {
                    final Path targetFile = Paths.get(inProgressBatchFolderPath.toString(), CONTENT_FILES, normalizedFilename);
                    try(final InputStream inStream = fileItemStream.openStream()){
                        FileUtils.copyInputStreamToFile(inStream, targetFile.toFile());
                        LOGGER.trace("Wrote content file '{}'", targetFile.toFile());
                    }
                    catch(IOException ex){
                        throw new StagingException(ex);
                    }
                }
                fileNames.add(normalizedFilename);
            }
            completeInProgressBatch(inProgressBatchFolderPath, batchId);
            return fileNames;
        }
        catch (IncompleteBatchException | InvalidBatchException | StagingException ex){
            LOGGER.error("Error saving batch [%s].", ex.getMessage());
            cleanupInProgressBatch(inProgressBatchFolderPath.toFile());
            throw ex;
        }
        catch (Throwable t){
            cleanupInProgressBatch(inProgressBatchFolderPath.toFile());
            throw new StagingException(t);
        }
    }

    private void cleanupInProgressBatch(final File inProgressBatchFolderPath){

        try {
            if(inProgressBatchFolderPath.exists()){
                LOGGER.error(String.format("Removing in progress batch [%s] after failure.", inProgressBatchFolderPath));
                FileUtils.deleteDirectory(inProgressBatchFolderPath);
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to remove in progress batch [%s] after failure.", inProgressBatchFolderPath));
        }
    }

    private void completeInProgressBatch(final Path inProgressBatchFolderPath, final String batchId)
            throws StagingException {

        final Path batchFolder = batchPathProvider.getPathForBatch(batchId);
        if(batchFolder.toFile().exists()){
            try {
                FileUtils.deleteDirectory(batchFolder.toFile());
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to delete existing batch [%s]", batchId));
                throw new StagingException(ex);
            }
        }

        try {
            FileUtils.moveDirectory(inProgressBatchFolderPath.toFile(), batchFolder.toFile());
        } catch (IOException ex) {
            LOGGER.error(String.format("Failed to move in progress batch [%s]", batchId));
            throw new StagingException(ex);
        }
    }

}
