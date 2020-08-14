/*
 * Copyright 2019-2020 Micro Focus or one of its affiliates.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "staging")
public class StagingProperties
{

    private String basePath;
    private int subbatchSize;
    private String storagePath;
    private int fieldValueSizeThreshold;
    private long fileAgeThreshold;
    private long fileCleanUpInterval;
    private boolean skipFileCleanUp;
    private int healthcheckTimeoutSeconds;

    public String getBasePath()
    {
        return basePath;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public int getSubbatchSize()
    {
        return subbatchSize;
    }

    public void setSubbatchSize(int subbatchSize)
    {
        this.subbatchSize = subbatchSize;
    }

    public String getStoragePath()
    {
        return storagePath;
    }

    public void setStoragePath(String storagePath)
    {
        this.storagePath = storagePath;
    }

    public int getFieldValueSizeThreshold()
    {
        return fieldValueSizeThreshold;
    }

    public void setFieldValueSizeThreshold(int fieldValueSizeThreshold)
    {
        this.fieldValueSizeThreshold = fieldValueSizeThreshold;
    }

    public long getFileAgeThreshold()
    {
        return fileAgeThreshold;
    }

    public void setFileAgeThreshold(final long fileAgeThreshold)
    {
        this.fileAgeThreshold = fileAgeThreshold;
    }

    public long getFileCleanUpInterval()
    {
        return fileCleanUpInterval;
    }

    public void setFileCleanUpInterval(final long fileCleanUpInterval)
    {
        this.fileCleanUpInterval = fileCleanUpInterval;
    }

    public boolean getSkipFileCleanUp()
    {
        return skipFileCleanUp;
    }

    public void setSkipFileCleanUp(final boolean skipFileCleanUp)
    {
        this.skipFileCleanUp = skipFileCleanUp;
    }

    public int getHealthcheckTimeoutSeconds()
    {
        return healthcheckTimeoutSeconds;
    }

    public void setHealthcheckTimeoutSeconds(final int healthcheckTimeoutSeconds)
    {
        this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    }
}
