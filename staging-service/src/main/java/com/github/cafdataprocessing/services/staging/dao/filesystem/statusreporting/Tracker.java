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

import com.github.cafdataprocessing.services.staging.models.InProgressMetrics;
import java.time.Instant;

final class Tracker
{
    private final Instant lastModifiedTime;
    private final Long numberOfBytesReceived;
    private final Long fileUploadRateInBytesPerSecond;
    private final boolean isProgressing;

    public Tracker()
    {
        this(null, null, null, false);
    }

    public Tracker(
        final Instant lastModifiedTime,
        final Long numberOfBytesReceived,
        final Long fileUploadRateInBytesPerSecond,
        final boolean isProgressing
    )
    {
        this.lastModifiedTime = lastModifiedTime;
        this.numberOfBytesReceived = numberOfBytesReceived;
        this.fileUploadRateInBytesPerSecond = fileUploadRateInBytesPerSecond;
        this.isProgressing = isProgressing;
    }

    public InProgressMetrics toInProgressMetrics()
    {
        final InProgressMetrics inProgressMetrics = new InProgressMetrics();
        inProgressMetrics.setLastModifiedDate(lastModifiedTime);
        inProgressMetrics.setBytesReceived(numberOfBytesReceived);
        inProgressMetrics.setBytesPerSecond(fileUploadRateInBytesPerSecond);
        inProgressMetrics.setIsProgressing(isProgressing);
        return inProgressMetrics;
    }

    @Override
    public String toString()
    {
        return "Tracker{"
            + "lastModifiedTime=" + lastModifiedTime
            + ", numberOfBytesReceived=" + numberOfBytesReceived
            + ", fileUploadRateInBytesPerSecond=" + fileUploadRateInBytesPerSecond
            + ", isProgressing=" + isProgressing
            + '}';
    }
}
