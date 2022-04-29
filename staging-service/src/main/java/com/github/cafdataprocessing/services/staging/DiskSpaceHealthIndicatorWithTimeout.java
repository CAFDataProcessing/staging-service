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
package com.github.cafdataprocessing.services.staging;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;

final class DiskSpaceHealthIndicatorWithTimeout extends DiskSpaceHealthIndicator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSpaceHealthIndicatorWithTimeout.class);

    private final File path;
    private final int healthcheckTimeoutSeconds;
    private final ExecutorService healthcheckExecutor;

    public DiskSpaceHealthIndicatorWithTimeout(
        final File path, final long threshold, final int healthcheckTimeoutSeconds)
    {
        super(path, threshold);
        this.path = path;
        this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
        this.healthcheckExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception
    {
        final Future<Void> healthcheckFuture = healthcheckExecutor.submit(() -> {
            DiskSpaceHealthIndicatorWithTimeout.super.doHealthCheck(builder);
            return null;
        });
        try {
            healthcheckFuture.get(healthcheckTimeoutSeconds, TimeUnit.SECONDS);
        } catch (final TimeoutException e) {
            healthcheckFuture.cancel(true);
            builder.down().withDetail(
                "errorMessage",
                String.format("Timeout after %s seconds trying to access batches directory: %s",
                              healthcheckTimeoutSeconds,
                              path.toString()));
        } catch (final InterruptedException | ExecutionException e) {
            healthcheckFuture.cancel(true);
            builder.down().withDetail(
                "errorMessage",
                String.format("Exception thrown trying to access batches directory: %s", path.toString()));
            LOGGER.warn("Exception thrown trying to access batches directory {} during healthcheck", path.toString(), e);
        }
    }
}
