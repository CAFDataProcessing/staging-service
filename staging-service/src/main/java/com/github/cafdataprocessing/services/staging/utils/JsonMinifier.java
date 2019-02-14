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
