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
import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

public final class BatchNameProvider
{
    private static final String SUBBATCH_FILE_SUFFIX = "-json.batch";
    private static final String DATE_TIME_ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
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
        TimeZone timeZone = TimeZone.getTimeZone(ZoneOffset.UTC);
        DateFormat dateFormat = new SimpleDateFormat(DATE_TIME_ISO_PATTERN);
        dateFormat.setTimeZone(timeZone);
        String currentTimeAsString = dateFormat.format(new Date());
        return cleanseTime(currentTimeAsString);
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

    private static String cleanseTime(final String utcFormattedTime)
    {
        return utcFormattedTime.replaceAll(":", "");
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
}
