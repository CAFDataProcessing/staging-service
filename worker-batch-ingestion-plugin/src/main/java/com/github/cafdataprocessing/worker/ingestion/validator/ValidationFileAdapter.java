/*
 * Copyright 2019-2022 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.worker.ingestion.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class ValidationFileAdapter
{
    private final JsonNode fieldsJsonObject;
    private final JsonNode typesJsonObject;

    public ValidationFileAdapter(final String file) throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode fileContents = mapper.readTree(getFileContents(file));
        this.fieldsJsonObject = fileContents.get("fields");
        this.typesJsonObject = fileContents.get("types");
    }

    public ArrayList<String> getFieldKeys()
    {
        final ArrayList<String> fieldKeys = new ArrayList<>();
        final Iterator<String> iterator = fieldsJsonObject.fieldNames();
        iterator.forEachRemaining(fieldKeys::add);

        return fieldKeys;
    }

    public Map<String, ArrayList<String>> getFlattenedFieldKeys()
    {
        final Map<String, ArrayList<String>> flattenedFields = new HashMap<>();
        for (final String fieldKey : getFieldKeys()) {
            final JsonNode field = fieldsJsonObject.get(fieldKey);
            if (field.has("objectEncoding")) {
                if (field.get("objectEncoding").asText().equals("flattened")) {
                    final JsonNode propertiesJsonObject = typesJsonObject.get(fieldKey.toLowerCase());
                    final ArrayList<String> fieldKeys = new ArrayList<>();
                    final Iterator<String> iterator = propertiesJsonObject.fieldNames();
                    iterator.forEachRemaining(fieldKeys::add);
                    flattenedFields.put(fieldKey, fieldKeys);
                }
            }
        }
        return flattenedFields;
    }

    static String getFileContents(final String filePath) throws IOException
    {
        try {
            final InputStream inputStream = Files.newInputStream(Paths.get(filePath));
            final StringBuilder sb = new StringBuilder();

            try ( BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();

        } catch (final IOException ex) {
            throw new IOException("Failed to read Validation File", ex);
        }
    }
}
