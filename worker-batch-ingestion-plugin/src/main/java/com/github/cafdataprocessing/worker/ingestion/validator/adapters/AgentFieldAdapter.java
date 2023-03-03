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

import java.util.Set;

public class AgentFieldAdapter implements Adapter
{
    /**
     * Purpose of this class is to have specific logic to turn the agentFields.json file into meaningful validation rules
     *
     * ie - a list of actual fields that are allowed this isn't straight forward as there are some "flattened" fields etc. (OCR and
     * METADATA_FILES)
     */

    @Override
    public Set<String> adapt(final String file) throws AdapterException
    {
        final JSONObject fileContents = new JSONObject(Adapter.getFileContents(file));
        final JSONObject fields = new JSONObject(fileContents.optString("fields"));
        return fields.keySet();

        // TODO - more logic to get all fields including flattened and nested
    }
}
