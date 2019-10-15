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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.io.FileUtils.openOutputStream;
import static org.apache.commons.io.IOUtils.EOF;

public final class StagingFileUtils
{
    private static final long SLEEP_TIME = getSleepTime();

    private StagingFileUtils()
    {
    }

    public static void sleepyCopyInputStreamToFile(final InputStream source, final File destination) throws IOException
    {
        if (SLEEP_TIME > 0) {
            copyInputStreamToFile(source, destination);
        } else {
            FileUtils.copyInputStreamToFile(source, destination);
        }
    }

    private static void copyInputStreamToFile(final InputStream source, final File destination) throws IOException
    {
        try (final InputStream in = source) {
            copyToFile(in, destination);
        }
    }

    private static void copyToFile(final InputStream source, final File destination) throws IOException
    {
        try (final InputStream in = source;
             final OutputStream out = openOutputStream(destination)) {
            copy(in, out);
        }
    }

    private static int copy(final InputStream input, final OutputStream output) throws IOException
    {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    private static long copyLarge(final InputStream input, final OutputStream output) throws IOException
    {
        return copy(input, output, 1024 * 4);
    }

    private static long copy(final InputStream input, final OutputStream output, final int bufferSize) throws IOException
    {
        return copyLarge(input, output, new byte[bufferSize]);
    }

    @SuppressWarnings("SleepWhileInLoop")
    private static long copyLarge(final InputStream input, final OutputStream output, final byte[] buffer) throws IOException
    {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();

                final InterruptedIOException interruptedIOException = new InterruptedIOException();
                interruptedIOException.bytesTransferred = count > Integer.MAX_VALUE ? -1 : (int) count;

                throw interruptedIOException;
            }
        }
        return count;
    }

    private static long getSleepTime()
    {
        final String delay = System.getenv("CAF_STAGING_SERVICE_WRITING_DELAY");

        return delay == null ? 0 : Long.parseLong(delay);
    }
}
