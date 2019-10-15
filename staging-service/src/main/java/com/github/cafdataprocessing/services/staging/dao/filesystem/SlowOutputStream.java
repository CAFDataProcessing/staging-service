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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public final class SlowOutputStream extends OutputStream
{
    private static final long SLEEP_TIME = getSleepTime();

    private final OutputStream out;

    public static OutputStream create(final OutputStream out)
    {
        return SLEEP_TIME > 0
            ? new SlowOutputStream(out)
            : out;
    }

    private SlowOutputStream(final OutputStream out)
    {
        this.out = out;
    }

    @Override
    public void close() throws IOException
    {
        out.close();
    }

    @Override
    public void flush() throws IOException
    {
        out.flush();
    }

    @Override
    public void write(final int b) throws IOException
    {
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        out.write(b);
        sleep();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        out.write(b, off, len);
        sleep();
    }

    private static long getSleepTime()
    {
        final String delay = System.getenv("CAF_STAGING_SERVICE_WRITING_DELAY");

        return delay == null ? 0 : Long.parseLong(delay);
    }

    private void sleep() throws InterruptedIOException
    {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
    }
}
