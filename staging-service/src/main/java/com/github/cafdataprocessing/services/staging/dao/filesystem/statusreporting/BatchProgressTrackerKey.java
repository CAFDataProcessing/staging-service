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
package com.github.cafdataprocessing.services.staging.dao.filesystem.statusreporting;

import com.github.cafdataprocessing.services.staging.BatchId;
import com.github.cafdataprocessing.services.staging.TenantId;
import java.util.Objects;

final class BatchProgressTrackerKey implements Comparable<BatchProgressTrackerKey>
{
    private final TenantId tenantId;
    private final BatchId batchId;
    private final long threadId;

    public BatchProgressTrackerKey(final TenantId tenantId, final BatchId batchId, final long threadId)
    {
        this.tenantId = Objects.requireNonNull(tenantId);
        this.batchId = Objects.requireNonNull(batchId);
        this.threadId = threadId;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (!(obj instanceof BatchProgressTrackerKey)) {
            return false;
        }

        final BatchProgressTrackerKey other = (BatchProgressTrackerKey) obj;

        return tenantId.equals(other.tenantId)
            && batchId.equals(other.batchId)
            && threadId == other.threadId;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + tenantId.hashCode();
        hash = 59 * hash + batchId.hashCode();
        hash = 59 * hash + Long.hashCode(threadId);
        return hash;
    }

    @Override
    public int compareTo(final BatchProgressTrackerKey other)
    {
        final int tenantCompareResult = tenantId.compareTo(other.tenantId);
        if (tenantCompareResult != 0) {
            return tenantCompareResult;
        }

        final int batchCompareResult = batchId.compareTo(other.batchId);
        if (batchCompareResult != 0) {
            return batchCompareResult;
        }

        return Long.compare(threadId, other.threadId);
    }
}
