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
package com.github.cafdataprocessing.services.staging.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.lang3.RandomStringUtils;

public class MultiPartDocument implements MultiPart
{
    private final String name;
    private final InputStreamSupplier streamSupplier;

    public MultiPartDocument(final URL resourceURL)
    {
        this.name = generateName();
        this.streamSupplier = resourceURL::openStream;
    }

    public MultiPartDocument(final File inputFile)
    {
        this.name = generateName();
        this.streamSupplier = () -> new FileInputStream(inputFile);
    }

    public MultiPartDocument(final InputStreamSupplier streamSupplier)
    {
        this.name = generateName();
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
        return "application/document+json";
    }

    @Override
    public InputStream openInputStream() throws IOException
    {
        return streamSupplier.get();
    }

    private String generateName()
    {
        return RandomStringUtils.randomAlphanumeric(10);
    }

}
