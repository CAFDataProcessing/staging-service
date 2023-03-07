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
package com.github.cafdataprocessing.services.staging.dao.filesystem.statusreporting;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import com.github.cafdataprocessing.services.staging.dao.filesystem.BatchPathProvider;
import com.github.cafdataprocessing.services.staging.exceptions.BatchNotFoundException;
import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import com.github.cafdataprocessing.services.staging.models.BatchStatus;
import com.github.cafdataprocessing.services.staging.models.BatchStatusResponse;
import com.github.cafdataprocessing.services.staging.models.InProgress;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BatchStatusProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchStatusProvider.class);

    private final BatchPathProvider batchPathProvider;

    public BatchStatusProvider(final BatchPathProvider batchPathProvider)
    {
        this.batchPathProvider = batchPathProvider;
    }

    public BatchStatusResponse getStatus(final TenantId tenantId, final BatchId batchId)
        throws BatchNotFoundException, StagingException, InterruptedException
    {
        // Get the directories where other services are progressing this batch
        final List<Path> otherServiceInProgressBatchDirs = getOtherServicesInProgressBatchDirectories(tenantId, batchId);

        // Get the progress reports of other services
        final List<Tracker> inProgressReports = new ArrayList<>();
        final List<StagingException> progressReportExceptions = new ArrayList<>();
        for (final Path inProgressBatchDir : otherServiceInProgressBatchDirs) {
            try {
                final Tracker progressReport = getProgressReportFromDirectory(inProgressBatchDir);
                inProgressReports.add(progressReport);
            } catch (final IOException ex) {
                final StagingException stagingException
                    = new StagingException("Failed to get progress report from directory: " + inProgressBatchDir, ex);
                progressReportExceptions.add(stagingException);
            }
        }

        // Add in the progress reports from this service
        inProgressReports.addAll(BatchProgressTracker.get(tenantId, batchId));

        // Check if the batch has been completed
        final boolean batchInCompletedState = batchPathProvider.getPathForBatch(tenantId, batchId).toFile().exists();

        if (!batchInCompletedState) {
            // Ignore IOExceptions that happened when retrieving the in-progress reports if the batch is completed,
            // as they were probably due to the batch completing while the report was being collected.
            if (!progressReportExceptions.isEmpty()) {
                for (final StagingException ex : progressReportExceptions) {
                    LOGGER.error("Failed to get progress report", ex);
                }
                throw progressReportExceptions.get(0);
            }

            if (inProgressReports.isEmpty()) {
                throw new BatchNotFoundException(batchId.getValue());
            }
        }

        assert batchInCompletedState || !inProgressReports.isEmpty();
        assert batchInCompletedState || progressReportExceptions.isEmpty();

        // Build and return the Response
        final InProgress inProgress = new InProgress();
        inProgress.setMetrics(inProgressReports.stream().map(Tracker::toInProgressMetrics).collect(Collectors.toList()));
        final BatchStatus batchStatus = new BatchStatus();
        batchStatus.setBatchComplete(batchInCompletedState);
        batchStatus.setInProgress(inProgress);
        final BatchStatusResponse response = new BatchStatusResponse();
        response.setBatchId(batchId.getValue());
        response.setBatchStatus(batchStatus);
        return response;
    }

    private List<Path> getOtherServicesInProgressBatchDirectories(final TenantId tenantId, final BatchId batchId)
        throws StagingException
    {
        final Path tenantInProgressDir = batchPathProvider.getTenantInprogressDirectory(tenantId);
        final String batchIdStr = batchId.getValue();
        final List<Path> list = new ArrayList<>();
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(tenantInProgressDir)) {
            for (final Path batch : directoryStream) {
                final BatchInProgressDirectoryName batchDir = new BatchInProgressDirectoryName(batch.getFileName().toString());
                if (!batchDir.isThisService() && batchDir.getBatchId().equals(batchIdStr)) {
                    list.add(batch);
                }
            }
        } catch (final IOException ex) {
            throw new StagingException("Error while traversing in-progress folder", ex);
        }
        return list;
    }

    private static Tracker getProgressReportFromDirectory(final Path batchDirectory)
        throws IOException, InterruptedException
    {
        final DirectoryInfo before = DirectoryInfo.create(batchDirectory);
        Thread.sleep(1000);
        final DirectoryInfo after = DirectoryInfo.create(batchDirectory);

        return new Tracker(
            after.getLastModifiedTime().toInstant(),
            after.getTotalSize(),
            after.getTotalSize() - before.getTotalSize(),
            after.getLastModifiedTime().compareTo(before.getLastModifiedTime()) > 0 || after.getTotalSize() > before.getTotalSize());
    }
}
