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

import java.time.Instant;

public class Tracker
{
    private Instant lastModifiedTime;
    private Long numberOfBytesReceived;
    private Long fileUploadRateInBytesPerSecond;
    private Instant uploadStartTime;

    private boolean isProgressing;

    public Tracker() {
        this.lastModifiedTime = null;
        this.numberOfBytesReceived = null;
        this.fileUploadRateInBytesPerSecond = null;
        this.uploadStartTime = null;
        this.isProgressing = false;
    }

    public Instant getUploadStartTime()
    {
        return uploadStartTime;
    }

    public void setUploadStartTime(final Instant uploadStartTime)
    {
        this.uploadStartTime = uploadStartTime;
    }

    public Instant getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(final Instant lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Long getNumberOfBytesReceived()
    {
        return numberOfBytesReceived;
    }

    public void setNumberOfBytesReceived(final long numberOfBytesReceived)
    {
        this.numberOfBytesReceived = numberOfBytesReceived;
    }

    public Long getFileUploadRateInBytesPerSecond()
    {
        return fileUploadRateInBytesPerSecond;
    }

    public void setFileUploadRateInBytesPerSecond(final long fileUploadRateInBytesPerSecond)
    {
        this.fileUploadRateInBytesPerSecond = fileUploadRateInBytesPerSecond;
    }

    public boolean isProgressing()
    {
        return isProgressing;
    }

    public void setProgressing(final boolean progressing)
    {
        isProgressing = progressing;
    }

    @Override
    public String toString()
    {
        return "Tracker{" +
                "lastModifiedTime=" + lastModifiedTime +
                ", numberOfBytesReceived=" + numberOfBytesReceived +
                ", fileUploadRateInBytesPerSecond=" + fileUploadRateInBytesPerSecond +
                ", uploadStartTime=" + uploadStartTime +
                ", isProgressing=" + isProgressing +
                '}';
    }
}