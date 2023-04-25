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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class ValidationFileAdapter
{
    private final JsonNode fieldsJsonNode;
    private final JsonNode typesJsonNode;

    public static List<String> getFieldKeyRegExs(final String file) throws IOException
    {
        final ValidationFileAdapter adapter = new ValidationFileAdapter(file);
        return adapter.getFieldKeyRegExs();
    }

    private ValidationFileAdapter(final String file) throws IOException
    {
        try (final InputStream input = Files.newInputStream(Paths.get(file))) {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode fileContents = mapper.readTree(input);

            this.fieldsJsonNode = fileContents.get("fields");
            this.typesJsonNode = fileContents.get("types");
        }
    }

    private List<String> getFieldKeyRegExs()
    {
        final ArrayList<String> fieldKeyRegExs = new ArrayList<>();
        addRegExsToList(fieldKeyRegExs, "^", fieldsJsonNode, "$");

        return fieldKeyRegExs;
    }

    private void addRegExsToList(final ArrayList<String> fieldKeyRegExs, final String prefix, final JsonNode node, final String suffix)
    {
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();
            final JsonNode objectEncodingNode = field.getValue().get("objectEncoding");
            if (objectEncodingNode != null && objectEncodingNode.asText().equals("flattened")) {
                addRegExsToList(fieldKeyRegExs, prefix, field, suffix);
            } else {
                final String fieldName = field.getKey();
                fieldKeyRegExs.add(prefix + Pattern.quote(fieldName) + suffix);
            }
        }
    }

    private void addRegExsToList(
        final ArrayList<String> fieldKeyRegExs,
        final String prefix,
        final Map.Entry<String, JsonNode> field,
        final String suffix
    )
    {
        final String fieldName = field.getKey();
        final StringBuilder sb = new StringBuilder(prefix);
        sb.append(Pattern.quote(fieldName));

        final String[] fieldType = field.getValue().get("type").asText().split("(?=\\[)", 2);
        final long count = fieldType[1].chars().filter(ch -> ch == '[').count();

        sb.append("_");
        for (int i = 0; i < count; i++) {
            sb.append("[^_]+_");
        }

        final JsonNode node = typesJsonNode.get(fieldType[0]);

        addRegExsToList(fieldKeyRegExs, sb.toString(), node, suffix);
    }
}
