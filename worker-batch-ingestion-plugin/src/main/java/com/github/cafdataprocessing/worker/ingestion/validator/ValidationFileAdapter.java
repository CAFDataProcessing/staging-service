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
package com.github.cafdataprocessing.worker.ingestion.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

final class ValidationFileAdapter
{
    private final JsonNode fieldsJsonNode;
    private final JsonNode typesJsonNode;

    public ValidationFileAdapter(final String file) throws IOException
    {
        try (final InputStream input = Files.newInputStream(Paths.get(file))) {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode fileContents = mapper.readTree(input);

            this.fieldsJsonNode = fileContents.get("fields");
            this.typesJsonNode = fileContents.get("types");
        }
    }

    public ArrayList<String> getFieldKeys()
    {
        final ArrayList<String> fieldKeys = new ArrayList<>();
        final Iterator<Map.Entry<String, JsonNode>> fieldIterator = fieldsJsonNode.fields();

        while (fieldIterator.hasNext()) {
            final Map.Entry<String, JsonNode> field = fieldIterator.next();
            final JsonNode objectEncodingNode = field.getValue().get("objectEncoding");
            if (objectEncodingNode != null && objectEncodingNode.asText().equals("flattened")) {
                fieldKeys.addAll(generateFlattenedFieldRegex(field));
            } else {
                final String newField = field.getKey();
                fieldKeys.add(Pattern.quote(newField));
            }
        }
        return fieldKeys;
    }

    private ArrayList<String> generateFlattenedFieldRegex(final Map.Entry<String, JsonNode> field)
    {
        final ArrayList<String> flattenedFields = new ArrayList<>();
        final ArrayList<String> stringsForField = new ArrayList<>(getStringsForField(field));

        for (final String s : stringsForField) {
            flattenedFields.add("^" + s + "$");
        }

        return flattenedFields;
    }

    private ArrayList<String> getStringsForField(final Map.Entry<String, JsonNode> field)
    {
        final ArrayList<String> strings = new ArrayList<>();
        final String fieldKey = field.getKey();
        StringBuilder sb = new StringBuilder(fieldKey);

        final String[] fieldType = field.getValue().get("type").asText().split("(?=\\[)", 2);
        final StringBuilder flatChars = new StringBuilder();
        final long count = fieldType[1].chars().filter(ch -> ch == '[').count();

        sb.append("_");
        for (int i = 0; i < count; i++) {
            flatChars.append("([^_]+)_");
        }
        sb.append(flatChars);

        final JsonNode node = typesJsonNode.get(fieldKey.toLowerCase());

        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> property = fields.next();

            final JsonNode objectEncodingNode = property.getValue().get("objectEncoding");
            if (objectEncodingNode != null && objectEncodingNode.asText().equals("flattened")) {
                for (final String s : getStringsForField(property)) {
                    strings.add(sb + s);
                }
            } else {
                sb.append(property.getKey());
                strings.add(sb.toString());
                sb = new StringBuilder(fieldKey);
                sb.append("_").append(flatChars);
            }
        }
        return strings;
    }
}
