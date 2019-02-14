/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
 */
package com.github.cafdataprocessing.services.staging;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;

import java.io.IOException;
import java.util.Iterator;

public class FileItemStreamIterator implements Iterator<FileItemStream> {

    private final FileItemIterator fileItemIterator;

    public FileItemStreamIterator(final FileItemIterator fileItemIterator){

        this.fileItemIterator = fileItemIterator;
    }

    @Override
    public boolean hasNext() {
        try {
            return fileItemIterator.hasNext();
        } catch (FileUploadException | IOException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileItemStream next() {
        try {
            return fileItemIterator.next();
        } catch (FileUploadException | IOException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }

}
