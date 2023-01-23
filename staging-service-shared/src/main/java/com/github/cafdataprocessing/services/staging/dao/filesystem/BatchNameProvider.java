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
package com.github.cafdataprocessing.services.staging.dao.filesystem;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.utils.ExtractedThreadAndServiceIDs;
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class BatchNameProvider
{
    private static final String SUBBATCH_FILE_SUFFIX = "-json.batch";
    private static final String DATE_TIME_ISO_PATTERN = "yyyy-MM-dd'T'HHmmss.SSSX";
    private static final DateTimeFormatter formatToday = DateTimeFormatter.ofPattern(DATE_TIME_ISO_PATTERN).withZone(ZoneOffset.UTC);

    public static String getBatchDirectoryName(final BatchId batchId)
    {
        return getCurrentTimeAsString()
            .concat("-").concat("" + Thread.currentThread().getId())
            .concat("-").concat(ServiceIdentifier.getServiceId())
            .concat("-").concat(batchId.getValue());
    }

    public static String getSubBatchName()
    {
        return getCurrentTimeAsString().concat(SUBBATCH_FILE_SUFFIX);
    }

    private static String getCurrentTimeAsString()
    {
        return formatToday.format(Instant.now());
    }

    public static long getFileCreationTime(final String fileName)
    {
        final String time = fileName.substring(0, 22);

        try {
            System.out.println("File creation time file name: " + time);
            return Instant.parse(reverseCleanseTimeString(time)).toEpochMilli();
        } catch (final DateTimeParseException ex) {
            return Instant.now().toEpochMilli();
        }
    }

    public static String getBatchId(final String fileName)
    {
        final int endOfThreadId = fileName.indexOf('-',23);
        return fileName.substring(fileName.indexOf('-', endOfThreadId+1)+1);
    }

    private static String reverseCleanseTimeString(final String timeString)
    {
        return new StringBuilder().append(timeString.substring(0, 13))
            .append(":")
            .append(timeString.substring(13, 15))
            .append(":")
            .append(timeString.substring(15)).toString();
    }

    public static boolean validateFileName(final String fileName)
    {
        return fileName.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{6}.[0-9]{3}Z-.*-.*-.*");
    }

    public static String extractThreadIDAndServiceID(final String fileName)
    {
        //Example File name: 2022-11-11T132455.509Z-28-2726eec0-test-batch10
        //return: 28-2726eec0  || ThreadID-ServiceID
        final int endOfThreadId = fileName.indexOf('-',23);
        final String threadID = fileName.substring(23,endOfThreadId);
        final String serviceID = fileName.substring(endOfThreadId+1, fileName.indexOf('-',endOfThreadId+1));
        return new ExtractedThreadAndServiceIDs(threadID, serviceID).getCombinedIDs();
    }
}
