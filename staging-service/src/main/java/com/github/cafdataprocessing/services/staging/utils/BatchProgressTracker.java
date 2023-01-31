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
package com.github.cafdataprocessing.services.staging.utils;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BatchProgressTracker
{
    private static final ConcurrentHashMap<String, Tracker> inProgressTrackerMap = new ConcurrentHashMap<>();

    public void updateTracker(final ServletFileUpload fileUpload)
    {
        fileUpload.setProgressListener(new ProgressListenerImpl());
    }

    public void remove(final long threadId, final String serviceId)
    {
        inProgressTrackerMap.remove(threadId + "-" + serviceId);
    }

    public void putAll(final Map<String, Tracker> tempMap)
    {
        inProgressTrackerMap.putAll(tempMap);
    }

    public void removeTrackerOfDifferentService()
    {
        inProgressTrackerMap.entrySet().removeIf(entry -> !entry.getKey().contains(ServiceIdentifier.getServiceId()));
    }

    public Tracker get(final String batchThread)
    {
        return inProgressTrackerMap.get(batchThread);
    }

    public void put(final String threadID, final Tracker tracker)
    {
        inProgressTrackerMap.put(threadID, tracker);
    }

    private static class ProgressListenerImpl implements ProgressListener
    {
        private final Tracker tracker;
        private long megaBytes;

        public ProgressListenerImpl()
        {
            final Tracker newTracker = new Tracker();
            newTracker.setUploadStartTime(Instant.now());
            this.tracker = newTracker;
            this.megaBytes = -1;
        }

        @Override
        public void update(final long totalBytesRead, final long contentLength, final int item)
        {
            long totalBytesInMBytes = totalBytesRead / 1_048_576;
            if (megaBytes == totalBytesInMBytes) {
                return;
            }
            megaBytes = totalBytesInMBytes;
            Instant now = Instant.now();
            long diff = ChronoUnit.MILLIS.between(tracker.getUploadStartTime(), now);
            if (diff > 0) {
                long speed = Math.floorDiv(totalBytesRead, diff);
                tracker.setFileUploadRateInBytesPerSecond(speed * 1000);
                tracker.setNumberOfBytesReceived(totalBytesRead);
                tracker.setLastModifiedTime(now);
                tracker.setProgressing(true);
                inProgressTrackerMap.put(Thread.currentThread().getId() + "-" + ServiceIdentifier.getServiceId(), tracker);
            }
        }
    }
}
