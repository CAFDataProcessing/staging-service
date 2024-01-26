/*
 * Copyright 2019-2024 Open Text.
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.commons.fileupload2.core.ProgressListener;

public final class BatchProgressListener implements ProgressListener, AutoCloseable
{
    private final BatchProgressTrackerKey trackerKey;
    private final Instant uploadStartTime;
    private long megaBytes;

    public BatchProgressListener(final TenantId tenantId, final BatchId batchId)
    {
        this.trackerKey = new BatchProgressTrackerKey(tenantId, batchId, Thread.currentThread().getId());
        this.uploadStartTime = Instant.now();
        this.megaBytes = -1;

        BatchProgressTracker.put(trackerKey, new Tracker());
    }

    @Override
    public void update(final long totalBytesRead, final long contentLength, final int item)
    {
        final long totalBytesInMBytes = totalBytesRead / 1_048_576;
        if (megaBytes == totalBytesInMBytes) {
            return;
        }
        megaBytes = totalBytesInMBytes;
        final Instant now = Instant.now();
        final long diff = ChronoUnit.MILLIS.between(uploadStartTime, now);
        if (diff > 0) {
            final long speed = Math.floorDiv(totalBytesRead, diff);
            final Tracker tracker = new Tracker(now, totalBytesRead, speed * 1000, true);
            BatchProgressTracker.put(trackerKey, tracker);
        }
    }

    @Override
    public void close()
    {
        BatchProgressTracker.remove(trackerKey);
    }
}
