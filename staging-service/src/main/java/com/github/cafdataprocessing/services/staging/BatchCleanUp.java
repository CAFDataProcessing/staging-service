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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("batchCleanUp")
public final class BatchCleanUp
{
    private static final Logger LOG = LoggerFactory.getLogger(BatchCleanUp.class);
    private final long threshold;
    private final String storagePath;
    private final boolean skipCleanUp;

    public BatchCleanUp(final StagingProperties properties)
    {
        this.storagePath = properties.getStoragePath();
        this.threshold = properties.getFileAgeThreshold();
        this.skipCleanUp = properties.getSkipFileCleanUp();
    }

    /**
     * Deletes batches directories that have not been modified in more time than the configured threshold allows.
     */
    public void deleteStaleBatches()
    {
        if (this.skipCleanUp) {
            return;
        }
        final File batchFileStorageDirectory = new File(this.storagePath);
        final File[] directoryIndex = batchFileStorageDirectory.listFiles();
        if (directoryIndex != null) {
            for (final File file : directoryIndex) {
                if (isDirectoryStale(file.lastModified(), this.threshold)) {
                    if (file.isDirectory()) {
                        deleteFilesInFolder(file);
                    }
                    LOG.debug("Deleting: {}", file.getPath());
                    file.delete();
                }
            }
        }
    }

    /**
     * Deletes all files within a given directory
     *
     * @param file Directory represented by a File object.
     */
    private static void deleteFilesInFolder(final File file)
    {
        if (file.list().length == 0) {
            return;
        }

        for (final File fileToDelete : file.listFiles()) {
            if (fileToDelete.isDirectory()) {
                deleteFilesInFolder(fileToDelete);
            }
            LOG.debug("Deleting: {}", file.getPath());
            fileToDelete.delete();
        }
    }

    /**
     * Determines if the current time minus the ageThreshold is larger than the last modified time supplied.
     *
     * @param lastModifiedTime Last time a file was modified
     * @param ageThreshold How long a file should remain unmodified before being removed
     * @return true if lastModifiedTime is larger than the current time minus the ageThreshold else it will return false
     */
    private static boolean isDirectoryStale(final long lastModifiedTime, final long ageThreshold)
    {
        return (System.currentTimeMillis() - ageThreshold) > lastModifiedTime;
    }
}
