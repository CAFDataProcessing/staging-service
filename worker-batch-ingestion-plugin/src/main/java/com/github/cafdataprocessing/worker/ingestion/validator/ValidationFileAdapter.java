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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

final class ValidationFileAdapter
{
    private final JsonNode fieldsJsonNode;
    private final JsonNode typesJsonNode;

    public ValidationFileAdapter(final String file) throws IOException
    {
        try ( InputStream input = Files.newInputStream(java.nio.file.Paths.get(file))) {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode fileContents = mapper.readTree(input);

            this.fieldsJsonNode = fileContents.get("fields");
            this.typesJsonNode = fileContents.get("types");
        } catch (final IOException e) {
            throw new IOException("Failed to read Validation File: " + file);
        }
    }

    public ArrayList<String> getFieldKeys()
    {
        final ArrayList<String> fieldKeys = new ArrayList<>();
        final Iterator<Map.Entry<String, JsonNode>> fieldIterator = fieldsJsonNode.fields();

        while (fieldIterator.hasNext()) {
            final String newField;
            final Map.Entry<String, JsonNode> field = fieldIterator.next();
            if (field.getValue().has("objectEncoding")) {
                if (field.getValue().get("objectEncoding").asText().equals("flattened")) {
                    fieldKeys.addAll(generateFlattenedFieldRegex(field));
                } else {
                    newField = field.getKey();
                    fieldKeys.add(newField);
                }
            } else {
                newField = field.getKey();
                fieldKeys.add(newField);
            }
        }
        return fieldKeys;
    }

    private ArrayList<String> generateFlattenedFieldRegex(final Map.Entry<String, JsonNode> field)
    {
        final ArrayList<String> flattenedFields = new ArrayList<>();

        final String[] fieldType = field.getValue().get("type").asText().split("(?<=\\[)|(?=\\[)", 2);
        final JsonNode property = typesJsonNode.get(fieldType[0]);
        final long count = fieldType[1].chars().filter(ch -> ch == '[').count();

        final Iterator<Map.Entry<String, JsonNode>> properties = property.fields();

        while (properties.hasNext()) {
            final String suffix = properties.next().getKey();
            final StringBuilder sb = new StringBuilder();
            sb.append("^").append("(").append(fieldType[0].toUpperCase()).append(")");

            sb.append("+(_(?=\\d))");
            for (int i = 0; i < count; i++) {
                sb.append("+((?<=_)\\d(?=_))+((?<=\\d)_)");
            }
            sb.append("+").append("(").append(suffix.toUpperCase()).append(")").append("$");

            flattenedFields.add(sb.toString());
            property.fields().next();
        }
        return flattenedFields;
    }
}
