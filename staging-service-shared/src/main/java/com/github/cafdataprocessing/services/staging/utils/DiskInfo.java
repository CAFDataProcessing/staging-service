/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

public class DiskInfo {
    private final long totalSpace;
    private final long usableSpace;
    private final long freeSpace;

    public DiskInfo(final long total, final long usable, final long free) {
        super();
        this.totalSpace = total;
        this.usableSpace = usable;
        this.freeSpace = free;
    }

    /**
     * @return the total
     */
    public long getTotalSpace() {
        return totalSpace;
    }

    /**
     * @return the usable
     */
    public long getUsableSpace() {
        return usableSpace;
    }

    /**
     * @return the free
     */
    public long getFreeSpace() {
        return freeSpace;
    }

}
