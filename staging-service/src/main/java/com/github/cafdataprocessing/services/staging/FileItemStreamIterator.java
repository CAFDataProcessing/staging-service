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
