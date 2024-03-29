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
package com.github.cafdataprocessing.services.staging.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MultiPartContent implements MultiPart
{

    private final String name;
    private final InputStreamSupplier streamSupplier;

    public MultiPartContent(final String name, final URL resourceURL)
    {
        this.name = name;
        this.streamSupplier = resourceURL::openStream;
    }

    public MultiPartContent(final File inputFile)
    {
        this.name = inputFile.getName();
        this.streamSupplier = () -> new FileInputStream(inputFile);
    }

    public MultiPartContent(final String name, final InputStreamSupplier streamSupplier)
    {
        this.name = name;
        this.streamSupplier = streamSupplier;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getContentType()
    {
        return "application/octet-stream";
    }

    @Override
    public InputStream openInputStream() throws IOException
    {
        return streamSupplier.get();
    }

}
