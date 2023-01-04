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
    private Long numberOfBytesReceived = null;
    private Long fileUploadRate = null;

    public Instant getUploadStartTime()
    {
        return uploadStartTime;
    }

    public void setUploadStartTime(Instant uploadStartTime)
    {
        this.uploadStartTime = uploadStartTime;
    }

    private Instant uploadStartTime;

    public Instant getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Instant lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Long getNumberOfBytesReceived()
    {
        return numberOfBytesReceived;
    }

    public void setNumberOfBytesReceived(long numberOfBytesReceived)
    {
        this.numberOfBytesReceived = numberOfBytesReceived;
    }

    public Long getFileUploadRate() {
        return fileUploadRate;
    }

    @Override
    public String toString()
    {
        return "Tracker{" +
                "lastModifiedTime=" + lastModifiedTime +
                ", numberOfBytesReceived=" + numberOfBytesReceived +
                ", fileUploadRate='" + fileUploadRate + '\'' +
                ", uploadStartTime=" + uploadStartTime +
                '}';
    }

    public void setFileUploadRate(long fileUploadRate)
    {
        this.fileUploadRate = fileUploadRate;
    }
}
