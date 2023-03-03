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
package com.github.cafdataprocessing.worker.ingestion.validator.adapters;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AgentFieldAdapter implements Adapter
{
    /**
     * Purpose of this class is to have specific logic to turn the agentFields.json file into meaningful validation rules
     *
     * ie - a list of actual fields that are allowed this isn't straight forward as there are some "flattened" fields etc. (OCR and
     * METADATA_FILES)
     */

    private final JSONObject fieldsJsonObject;
    private final JSONObject typesJsonObject;

    public AgentFieldAdapter(final String file) throws AdapterException
    {
        final JSONObject fileContents = new JSONObject(Adapter.getFileContents(file));
        this.fieldsJsonObject = new JSONObject(fileContents.optString("fields"));
        this.typesJsonObject = new JSONObject(fileContents.optString("types"));
    }

    @Override
    public Set<String> getFieldKeys()
    {
        return fieldsJsonObject.keySet();
    }

    @Override
    public Map<String, Set<String>> getFlattenedFields()
    {
        final Map<String, Set<String>> flattenedFields = new HashMap<>();
        for (String fieldKey : getFieldKeys()) {
            final JSONObject field = fieldsJsonObject.getJSONObject(fieldKey);
            if(field.optString("objectEncoding").equals("flattened")) {
                final JSONObject propertiesJsonObject = typesJsonObject.getJSONObject(fieldKey.toLowerCase());
                flattenedFields.put(fieldKey, propertiesJsonObject.keySet());
            }
        }
        return flattenedFields;
    }
}
