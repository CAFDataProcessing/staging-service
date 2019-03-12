package com.github.cafdataprocessing.services.staging.utils;

import java.io.File;

import com.github.cafdataprocessing.services.staging.exceptions.PathException;

public final class DiskSpaceChecker {
    private DiskSpaceChecker(){}

    public final static DiskInfo getDiskInformation(final String path) throws PathException
    {
        final File folder = new File(path);
        if(folder.exists() && folder.isDirectory())
        {
            return new DiskInfo(folder.getTotalSpace(), folder.getUsableSpace(), folder.getFreeSpace());
        }
        throw new PathException(path);
    }

}
