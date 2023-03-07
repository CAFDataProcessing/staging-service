/*
 * Copyright 2019-2023 Open Text.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

final class DiskAccessHealthIndicatorWithTimeout extends AbstractHealthIndicator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskAccessHealthIndicatorWithTimeout.class);

    private final Path healthcheckFile;
    private final int healthcheckTimeoutSeconds;
    private final ExecutorService healthcheckExecutor;

    public DiskAccessHealthIndicatorWithTimeout(
        final Path path, final int healthcheckTimeoutSeconds)
    {
        super();
        this.healthcheckFile = path.resolve("healthcheck-file.txt");
        this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
        this.healthcheckExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception
    {
        final Future<Void> healthCheckDiscAccessFuture = healthcheckExecutor.submit(() -> {
            doWriteCheck(builder);
            return null;
        });

        try {
            healthCheckDiscAccessFuture.get(healthcheckTimeoutSeconds, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            healthCheckDiscAccessFuture.cancel(true);
            builder.down().withDetail(
                "errorMessage",
                String.format("Timeout after %s seconds trying to access batches directory: %s",
                              healthcheckTimeoutSeconds,
                              healthcheckFile.toString()));
        } catch (final InterruptedException | ExecutionException e) {
            healthCheckDiscAccessFuture.cancel(true);
            builder.down().withDetail(
                "errorMessage",
                String.format("Exception thrown trying to access batches directory: %s", healthcheckFile));
            LOGGER.warn("Exception thrown trying to access batches directory {} during healthcheck", healthcheckFile.toString(), e);
        }
    }

    private synchronized void doWriteCheck(final Health.Builder builder) throws IOException
    {
        Path created = null;

        try {
            Files.deleteIfExists(healthcheckFile);
        } catch (final IOException e) {
            //Ignoring exception here due to wanting the healthcheck to only fail on the write check
        }

        try {
            //Create new healthcheck file for current healtcheck run
            created = Files.createFile(healthcheckFile);
            if (created == null) {
                builder.down().withDetail(
                    "errorMessage",
                    String.format("Exception thrown trying to write healthcheck file to directory %s",
                                  healthcheckFile.toString()));
                LOGGER.warn("Error trying to write health check file to directory {} during healthcheck",
                            healthcheckFile.toString());
            } else {
                builder.up();
            }
        } catch (final IOException e) {
            builder.down().withDetail(
                "errorMessage",
                String.format("Exception thrown trying to write healthcheck file to directory %s",
                              healthcheckFile.toString()));
            LOGGER.warn("Exception thrown trying to write healthcheck file to directory {} during healthcheck",
                        healthcheckFile.toString(), e);
        } finally {
            Files.deleteIfExists(created);
        }
    }
}
