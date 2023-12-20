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

import com.github.cafdataprocessing.services.staging.utils.ServiceIdentifier;

/**
 * Example Batch directory name:
 * <pre>{@code
 * 2022-11-11T132455.509Z-28-2726eec0-test-batch10
 * }</pre>
 */
final class BatchInProgressDirectoryName
{
    private static final int THREAD_ID_POS = 23;

    private final String directoryName;
    private final int serviceIdPos;
    private final int batchIdPos;

    public BatchInProgressDirectoryName(final String batchDirectoryName)
    {
        this.directoryName = batchDirectoryName;
        this.serviceIdPos = directoryName.indexOf('-', THREAD_ID_POS) + 1;
        this.batchIdPos = directoryName.indexOf('-', serviceIdPos) + 1;
    }

    private String getThreadId()
    {
        return directoryName.substring(THREAD_ID_POS, serviceIdPos - 1);
    }

    private String getServiceId()
    {
        return directoryName.substring(serviceIdPos, batchIdPos - 1);
    }

    public String getBatchId()
    {
        return directoryName.substring(batchIdPos);
    }

    public boolean isThisService()
    {
        return ServiceIdentifier.getServiceId().equals(getServiceId());
    }
}
