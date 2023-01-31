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

public final class ExtractedThreadAndServiceIDs
{
    private String threadID;
    private String serviceID;

    public ExtractedThreadAndServiceIDs(final String threadID, final String serviceID)
    {
        this.threadID = threadID;
        this.serviceID = serviceID;
    }

    public String getThreadID()
    {
        return threadID;
    }

    public void setThreadID(final String threadID)
    {
        this.threadID = threadID;
    }

    public String getServiceID()
    {
        return serviceID;
    }

    public void setServiceID(final String serviceID)
    {
        this.serviceID = serviceID;
    }

    public String getCombinedIDs()
    {
        return this.threadID + "-" + this.serviceID;
    }
}
