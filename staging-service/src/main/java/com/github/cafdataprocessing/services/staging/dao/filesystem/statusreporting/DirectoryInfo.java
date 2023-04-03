/*
 * Copyright 2019-2023 Open Text.
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;

final class DirectoryInfo
{
    private final long totalSize;
    private final FileTime lastModifiedTime;

    public static DirectoryInfo create(final Path dir) throws IOException
    {
        final Builder builder = new Builder(Files.getLastModifiedTime(dir));
        builder.includeDirectory(dir);

        return builder.build();
    }

    private DirectoryInfo(final long totalSize, final FileTime lastModifiedTime)
    {
        this.totalSize = totalSize;
        this.lastModifiedTime = lastModifiedTime;
    }

    public long getTotalSize()
    {
        return totalSize;
    }

    public FileTime getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    private static final class Builder
    {
        private long totalSize;
        private FileTime lastModifiedTime;

        public Builder(final FileTime lastModifiedTime)
        {
            this.totalSize = 0;
            this.lastModifiedTime = lastModifiedTime;
        }

        public void includeDirectory(final Path dir) throws IOException
        {
            final FileSystemProvider fileSystemProvider = dir.getFileSystem().provider();
            includeDirectory(fileSystemProvider, dir);
        }

        private void includeDirectory(final FileSystemProvider fileSystemProvider, final Path dir) throws IOException
        {
            try (final DirectoryStream<Path> files = fileSystemProvider.newDirectoryStream(dir, path -> true)) {
                for (final Path file : files) {
                    final BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
                    if (fileAttributes.isRegularFile()) {
                        includeFile(fileAttributes.size(), fileAttributes.lastModifiedTime());
                    } else if (fileAttributes.isDirectory()) {
                        includeDirectory(fileSystemProvider, file);
                    }
                }
            }
        }

        private void includeFile(final long fileSize, final FileTime fileLastModifiedTime)
        {
            totalSize += fileSize;
            if (fileLastModifiedTime.compareTo(lastModifiedTime) > 0) {
                lastModifiedTime = fileLastModifiedTime;
            }
        }

        public DirectoryInfo build()
        {
            return new DirectoryInfo(totalSize, lastModifiedTime);
        }
    }
}
