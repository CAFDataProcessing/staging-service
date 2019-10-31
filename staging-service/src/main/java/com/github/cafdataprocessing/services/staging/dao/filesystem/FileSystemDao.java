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

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.dao.BatchDao;
import com.github.cafdataprocessing.services.staging.exceptions.BatchNotFoundException;
import com.github.cafdataprocessing.services.staging.exceptions.IncompleteBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidBatchException;
import com.github.cafdataprocessing.services.staging.exceptions.InvalidTenantIdException;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
public class FileSystemDao implements BatchDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDao.class);

    private static final String DOCUMENT_JSON_CONTENT = "application/document+json";

    private static final String CONTENT_FILES = "files";

    private final BatchPathProvider batchPathProvider;
    private final int subbatchSize;
    private final String storagePath;
    private final String basePath;
    private final int fieldValueSizeThreshold;
    private final long fileAgeThreshold;
    private final boolean skipBatchFileCleanup;

    public FileSystemDao(final String basePath, final int subbatchSize,
                         final String storagePath, final int fieldValueSizeThreshold,
                         final long fileAgeThreshold, final boolean skipBatchFileCleanup) {
        batchPathProvider = new BatchPathProvider(basePath);
        this.subbatchSize = subbatchSize;
        this.storagePath = storagePath;
        this.basePath = basePath;
        this.fieldValueSizeThreshold = fieldValueSizeThreshold;
        this.skipBatchFileCleanup = skipBatchFileCleanup;
        this.fileAgeThreshold = fileAgeThreshold;
    }

    @Override
    public List<String> getBatches(
            final TenantId tenantId,
            final String startsWith,
            final BatchId from,
            final Integer limit) throws StagingException {

        LOGGER.debug("Fetching batches starting with : {}", startsWith);

        final Path batchesPath = batchPathProvider.getPathForBatches(tenantId);
        if(!batchesPath.toFile().exists()){
            return new ArrayList<>();
        }
        //Retrieve the current list of batches in alphabetical order
        try(final Stream<Path> pathStream =
                    Files.walk(batchesPath, 1, FileVisitOption.FOLLOW_LINKS).skip(1)){

            Stream<String> batchDirectoryNames = pathStream.map(Path::toFile).
                    filter(File::isDirectory).map(File::getName);

            if(startsWith!=null){
                batchDirectoryNames = batchDirectoryNames.filter(f -> f.startsWith(startsWith));
            }

            if(from!=null){
                batchDirectoryNames = batchDirectoryNames.filter(f -> f.compareTo(from.getValue())>=0);
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
    public void deleteBatch(final TenantId tenantId, final BatchId batchId)
            throws BatchNotFoundException, StagingException {
        final Path batchPath = batchPathProvider.getPathForBatch(tenantId, batchId);
        if(!batchPath.toFile().exists()){
            throw new BatchNotFoundException(batchId.getValue());
        }

        try {
            FileUtils.deleteDirectory(batchPath.toFile());
        } catch (IOException ex) {
            throw new StagingException(ex);
        }
    }

    @Override
    public List<String> saveFiles(final TenantId tenantId, BatchId batchId, FileItemIterator fileItemIterator)
            throws StagingException, InvalidBatchException, IncompleteBatchException {

        final Path inProgressBatchFolderPath = batchPathProvider.getInProgressPathForBatch(tenantId, batchId);
        final Path storageRefFolderPath = batchPathProvider.getStorageRefFolderPathForBatch(tenantId, batchId, this.storagePath, CONTENT_FILES);
        final List<String> fileNames = new ArrayList<>();
        final Map<String, String> binaryFilesUploaded = new HashMap<>();
        try(final SubBatchWriter subBatchWriter = new SubBatchWriter(inProgressBatchFolderPath.toFile(), subbatchSize)){
            while(true){

                final FileItemStream fileItemStream;
                try{
                    LOGGER.debug("Retrieving next part...");
                    if (!fileItemIterator.hasNext()){
                        LOGGER.debug("No further parts.");
                        break;
                    }
                    fileItemStream = fileItemIterator.next();
                }
                catch (FileUploadException | IOException ex){
                    throw new IncompleteBatchException(ex);
                }

                if(!fileItemStream.isFormField()){
                    LOGGER.error("A form field is required.");
                    throw new InvalidBatchException("A form field is required.");
                }

                final String filename = fileItemStream.getFieldName();
                if(filename==null || filename.trim().length()==0){
                    LOGGER.error("The form field name must be present and contain the filename.");
                    throw new InvalidBatchException("The form field name must be present and contain the filename.");
                }
                final String contentType = fileItemStream.getContentType();
                if (contentType.equalsIgnoreCase(DOCUMENT_JSON_CONTENT)) {
                    LOGGER.debug("Part type: document; field name: {}", filename);
                    subBatchWriter
                        .writeDocumentFile(fileItemStream::openStream,
                                           storageRefFolderPath.toString(),
                                           Paths.get(inProgressBatchFolderPath.toString(), CONTENT_FILES).toString(),
                                           fieldValueSizeThreshold, binaryFilesUploaded);
                    fileNames.add(filename);
                } else {
                    LOGGER.debug("Part type: loose file; field name: {}", filename);
                    final String fileExtension = FilenameUtils.getExtension(filename);
                    final String targetFileName = fileExtension.isEmpty()
                        ? UUID.randomUUID().toString() : UUID.randomUUID().toString() + "." + fileExtension;
                    final File targetFile = Paths.get(inProgressBatchFolderPath.toString(), CONTENT_FILES, targetFileName).toFile();
                    LOGGER.debug("Reading loose file...");
                    try (final InputStream inStream = fileItemStream.openStream()) {
                        FileUtils.copyInputStreamToFile(inStream, targetFile);
                        LOGGER.debug("Loose file written to {}", targetFile);
                        fileNames.add(targetFileName);
                        binaryFilesUploaded.put(filename, targetFileName);
                    } catch (IOException ex) {
                        throw new StagingException(ex);
                    }
                }
            }
        }
        catch (IncompleteBatchException | InvalidBatchException | StagingException ex){
            LOGGER.error(String.format("Error saving batch [%s].", ex.getMessage()));
            cleanupInProgressBatch(inProgressBatchFolderPath.toFile());
            throw ex;
        }
        catch (Throwable t){
            LOGGER.error(String.format("Error saving batch [%s].", t.getMessage()));
            cleanupInProgressBatch(inProgressBatchFolderPath.toFile());
            throw new StagingException(t);
        }
        completeInProgressBatch(tenantId, inProgressBatchFolderPath, batchId);
        return fileNames;
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

    private void completeInProgressBatch(final TenantId tenantId, final Path inProgressBatchFolderPath, final BatchId batchId)
            throws StagingException {
        LOGGER.info("Completing batch with id {} for {}...", batchId, tenantId);

        final Path batchFolder = batchPathProvider.getPathForBatch(tenantId, batchId);
        if(batchFolder.toFile().exists()){
            LOGGER.warn("Batch {} has been previously uploaded.  Removing previously uploaded batch...", batchId);
            try {
                FileUtils.deleteDirectory(batchFolder.toFile());
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to delete existing batch [%s]", batchId));
                throw new StagingException(ex);
            }
        }

        LOGGER.debug("Moving {} to completed folder...", batchId);

        try {
            FileUtils.moveDirectory(inProgressBatchFolderPath.toFile(), batchFolder.toFile());
        } catch (IOException ex) {
            LOGGER.error(String.format("Failed to move in progress batch [%s]", batchId));
            throw new StagingException(ex);
        }

        LOGGER.debug("Batch {} completed successfully.", batchId);
    }

    /**
     *
     */
    @Scheduled(fixedDelayString = "${staging.fileCleanUpInterval}")
    @Override
    public void cleanUpStaleInprogressBatches()
    {
        if (skipBatchFileCleanup) {
            return;
        }
        // "Each mapped stream is closed after its contents have been placed into this stream."-
        try (final Stream<Path> batchesToClean = Files.list(Paths.get(basePath))
            .map(p -> getTenantInprogressDirectorySafely(p.getFileName().toString()))
            .flatMap(p -> getAllBatchFilesForAllDirectories(p.get()))
            .filter(p -> BatchNameProvider.validateFileName(p.getFileName().toString()))
            .filter(p -> shouldDelete(p.getFileName().toString()))
            .filter(p -> checkAllSubfilesSafely(p))) {

            while (batchesToClean.iterator().hasNext()) {
                final Path path = batchesToClean.iterator().next();
                try {
                    FileUtils.deleteDirectory(path.toFile());
                } catch (final IOException | IllegalArgumentException ex) {
                    LOGGER.error("Unable to delete directory {}", path);
                    LOGGER.debug("An error occured while attempting to delete folder {}", path, ex);
                }
            }
        } catch (final IOException ex) {
            LOGGER.error("An exception occured trying to read the files in the base directory.", ex);
        }
    }

    private boolean checkAllSubfilesSafely(final Path path)
    {
        try {
            return Files.list(path)
                .filter(p -> BatchNameProvider.validateFileName(p.getFileName().toString()))
                .filter(p -> !shouldDelete(p.getFileName().toString()))
                .collect(Collectors.toList())
                .isEmpty();
        } catch (final IOException ex) {
            LOGGER.error("Unable to open directory {}", path, ex);
            return false;
        }
    }

    /**
     * Obtain a path to the tenants in_progress folder, if the string provided is not a valid tenant id this method will return 
     * an empty Optional
     * @param tenantIdFolderName The name of the tenant
     * @return An Optional of paths to the tenants in_progress folder
     */
    private Optional<Path> getTenantInprogressDirectorySafely(final String tenantIdFolderName)
    {
        try{
            final TenantId tenantId = new TenantId(tenantIdFolderName);
            return Optional.ofNullable(batchPathProvider.getTenantInprogressDirectory(tenantId));
        } catch (final InvalidTenantIdException ex){
            LOGGER.debug("Ignoring folder {} as it does not represent a valid tenantId.", tenantIdFolderName);
            return Optional.empty();
        }
    }

    /**
     * Returns a list of the path to all sub files in all folders provide
     * @param tenantFolders List of Path objects that represent all current tenant in_progress folders
     * @return return a Stream of paths to all subfiles under all folders provided
     */
    private Stream<Path> getAllBatchFilesForAllDirectories(final Path tenantFolder)
    {
        try {
            return Files.list(tenantFolder);
        } catch (final IOException ex) {
            LOGGER.error("Unable to list files in directory {}", tenantFolder, ex);
            return Stream.empty();
        }
    }

     /**
     * Determines if the file was created longer ago than the file age threshold
     *
     * @param filename A filename to be used to obtain the file creation time
     * @return true if file creation time is longer than the file age threshold ago
     */
    private boolean shouldDelete(final String filename)
    {
        final long fileCreationTime = BatchNameProvider.getFileCreationTime(filename);
        return (Instant.now().toEpochMilli() - fileAgeThreshold) >= fileCreationTime;
    }
}
