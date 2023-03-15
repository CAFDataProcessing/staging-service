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

import java.io.IOException;
import java.io.InputStream;
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
        final InputStream input = ValidationFileAdapter.class.getClassLoader().getResourceAsStream(file);

        if (input != null) {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode fileContents = mapper.readTree(input);

            this.fieldsJsonObject = fileContents.get("fields");
            this.typesJsonObject = fileContents.get("types");
        } else {
            throw new IOException("Failed to read Validation File " + file);
        }

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
}
