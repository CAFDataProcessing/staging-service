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
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

final class BatchProgressTracker
{
    private static final ConcurrentSkipListMap<BatchProgressTrackerKey, Tracker> inProgressTrackerMap = new ConcurrentSkipListMap<>();

    private BatchProgressTracker()
    {
    }

    public static void put(final BatchProgressTrackerKey key, final Tracker tracker)
    {
        inProgressTrackerMap.put(key, tracker);
    }

    public static void remove(final BatchProgressTrackerKey key)
    {
        inProgressTrackerMap.remove(key);
    }

    public static Collection<Tracker> get(final TenantId tenantId, final BatchId batchId)
    {
        final BatchProgressTrackerKey fromKey = new BatchProgressTrackerKey(tenantId, batchId, Long.MIN_VALUE);
        final BatchProgressTrackerKey toKey = new BatchProgressTrackerKey(tenantId, batchId, Long.MAX_VALUE);

        return inProgressTrackerMap.subMap(fromKey, true, toKey, true).values();
    }
}
