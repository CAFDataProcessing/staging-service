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
