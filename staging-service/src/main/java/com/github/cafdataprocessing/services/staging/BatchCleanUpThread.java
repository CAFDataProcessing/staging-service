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

public final class BatchCleanUpThread implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(BatchCleanUpThread.class);

    @Override
    public void run()
    {
        final long threshold = Long.parseLong(getEnvVariableOrDefault("CAF_STAGING_SERVICE_FILE_AGE_THRESHOLD", "1")) * 60 * 60 * 1000;
        final long waitPeriod = Long.parseLong(
                                             getEnvVariableOrDefault("CAF_STAGING_SERVICE_FILE_CLEAN_UP_INTERVAL", "1")) * 60 * 60 * 1000;
        final String storageFilPath = System.getenv("CAF_STAGING_SERVICE_STORAGEPATH");
        while (true) {
            recurseAndDeleteOldBatches(threshold, storageFilPath);
            try {
                Thread.sleep(waitPeriod * 60 * 60 * 1000);
            } catch (final InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void recurseAndDeleteOldBatches(final long threshold, final String storageFilePath)
    {
        final File directory = new File(storageFilePath);
        final File[] directoryListing = directory.listFiles();
        for (final File batchFile : directoryListing) {
            if (batchFile.isDirectory()) {
                recurseAndDeleteOldBatches(threshold, batchFile.getPath());
                if (batchFile.list().length == 0) {
                    LOG.debug("Directory now empty, deleting directory {}", batchFile.getAbsolutePath());
                    batchFile.delete();
                }
            }
            if (batchFile.isFile()) {
                if (isFileOlderThan(batchFile.lastModified(), threshold)) {
                    LOG.debug("File is older than threshold of {}, deleting file {}", threshold, batchFile.getAbsolutePath());
                    batchFile.delete();
                }
            }
        }
    }

    private static boolean isFileOlderThan(final long lastModifiedTime, final long ageThreshold)
    {
        return (System.currentTimeMillis() - ageThreshold) > lastModifiedTime;
    }

    public static String getEnvVariableOrDefault(final String environmentKey, final String defaultValue)
    {
        return System.getenv(environmentKey) != null ? System.getenv(environmentKey) : defaultValue;
    }
}
