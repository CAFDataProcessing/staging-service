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
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BatchCleanUpThread implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(BatchCleanUpThread.class);

    /**
     * Runs the stale batch clean up thread
     * 
     * This thread checks the file system at set intervals to remove stale batch files left behind unintentionally
     */
    @Override
    public void run()
    {
        final long threshold = convertTimeToLong(getEnvVariableOrDefault("CAF_STAGING_SERVICE_FILE_AGE_THRESHOLD", "1"),
                                                 getEnvVariableOrDefault("CAF_STAGING_SERVICE_FILE_AGE_THRESHOLD_TIME_UNIT", "hours"));
        final long waitPeriod = convertTimeToLong(getEnvVariableOrDefault("CAF_STAGING_SERVICE_FILE_CLEAN_UP_INTERVAL", "1"),
                                                  getEnvVariableOrDefault("CAF_STAGING_SERVICE_CLEAN_UP_INTERVAL_TIME_UNIT", "hours"));
        final String storageFilPath = getEnvVariableOrDefault("CAF_STAGING_SERVICE_STORAGEPATH", "/etc/store/batches/");
        while (true) {
            deleteStaleBatches(threshold, storageFilPath);
            try {
                Thread.sleep(waitPeriod);
            } catch (final InterruptedException ex) {
                LOG.error("Clean up thread caught InterruptedException: ", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Deletes batches directories that have not been modified in more time than the configured threshold allows.
     *
     * @param threshold How long a file can remain unmodified before it will be removed
     * @param storageFilePath The base path for the batches directory
     */
    private static void deleteStaleBatches(final long threshold, final String storageFilePath)
    {
        Objects.requireNonNull(storageFilePath);
        final File batchFileStorageDirectory = new File(storageFilePath);
        final File[] directoryIndex = batchFileStorageDirectory.listFiles();
        if (directoryIndex != null) {
            for (final File file : directoryIndex) {
                if (file.isDirectory()) {
                    if (isDirectoryStale(file.lastModified(), threshold)) {
                        deleteFilesInFolder(file);
                        file.delete();
                    }
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

    /**
     * Retrieves and returns the environment variable value for the given key, else returns the default value supplied if no environment
     * variable was set for the supplied key
     *
     * @param environmentKey Key to use in lookup
     * @param defaultValue default value to return if no key was found in the environment
     * @return Either the value picked up from the environment or the default value if the key could not be found in the environment
     */
    public static String getEnvVariableOrDefault(final String environmentKey, final String defaultValue)
    {
        return System.getenv(environmentKey) != null ? System.getenv(environmentKey) : defaultValue;
    }

    /**
     * Converts the supplied string time to a long value using the time units to calculate the time specified in milliseconds.
     *
     * @param time The time to be converted
     * @param timeUnit The time unit to use when converting to milliseconds
     * @return The time converted to milliseconds
     */
    private static long convertTimeToLong(final String time, final String timeUnit)
    {
        final double timeValue = Double.parseDouble(time);
        switch (timeUnit.toUpperCase(Locale.US)) {
            case "HOURS": {
                return (long) (timeValue * 60 * 60 * 1000);
            }
            case "MINUTES": {
                return (long) (timeValue * 60 * 1000);
            }
            case "SECONDS": {
                return (long) (timeValue * 1000);
            }
            case "MILLISECONDS": {
                return (long) timeValue;
            }
            default: {
                return (long) (timeValue * 60 * 60 * 1000);
            }
        }
    }
}
