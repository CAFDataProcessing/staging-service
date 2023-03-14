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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class ValidationFileAdapter
{
    private final JSONObject fieldsJsonObject;
    private final JSONObject typesJsonObject;

    public ValidationFileAdapter(final String file) throws IOException
    {
        final JSONObject fileContents = new JSONObject(getFileContents(file));
        this.fieldsJsonObject = new JSONObject(fileContents.optString("fields"));
        this.typesJsonObject = new JSONObject(fileContents.optString("types"));
    }

    public Set<String> getFieldKeys()
    {
        return fieldsJsonObject.keySet();
    }

    public Map<String, Set<String>> getFlattenedFieldKeys()
    {
        final Map<String, Set<String>> flattenedFields = new HashMap<>();
        for (final String fieldKey : getFieldKeys()) {
            final JSONObject field = fieldsJsonObject.getJSONObject(fieldKey);
            if (field.optString("objectEncoding").equals("flattened")) {
                final JSONObject propertiesJsonObject = typesJsonObject.getJSONObject(fieldKey.toLowerCase());
                flattenedFields.put(fieldKey, propertiesJsonObject.keySet());
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
