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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public final class JsonMinifier {
    private JsonMinifier(){}

    public static final void minifyJson(final InputStream inputStream, final OutputStream outstream) throws IOException
    {
        final JsonFactory factory = new JsonFactory();
        final JsonParser parser = factory.createParser(inputStream);
        final JsonGenerator gen = factory.createGenerator(outstream);
        while (parser.nextToken() != null) {
            gen.copyCurrentEvent(parser);
        }
        outstream.write(System.lineSeparator().getBytes());
    }

}
