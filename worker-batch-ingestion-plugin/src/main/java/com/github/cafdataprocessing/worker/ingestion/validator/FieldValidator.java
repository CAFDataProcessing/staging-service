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

import com.github.cafdataprocessing.worker.ingestion.validator.adapters.AdapterException;
import com.github.cafdataprocessing.worker.ingestion.validator.adapters.AgentFieldAdapter;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FieldValidator {
    final Set<String> allowedFields;

    public FieldValidator(final String validationFile) throws AdapterException
    {
        AgentFieldAdapter adapter = new AgentFieldAdapter();
        allowedFields = adapter.adapt(validationFile);
    }

    public DocumentWorkerDocument validate(final DocumentWorkerDocument document)
    {
        for(final Map.Entry<String, List<DocumentWorkerFieldValue>> pair : document.fields.entrySet()) {
            boolean found = false;
            for(final String allowedField : allowedFields) {
                if (pair.getKey().equals(allowedField)) {
                    found = true;
                    break;
                }
            }

            if(!found) {
                final DocumentWorkerFailure fieldNotAllowedFailure = new DocumentWorkerFailure();
                fieldNotAllowedFailure.failureId = "FIELD-NOT-ALLOWED-FAILURE";
                fieldNotAllowedFailure.failureMessage
                        = String.format(pair.getKey() + " is not allowed to be set by the agent");
                document.failures = new ArrayList<>();
                document.failures.add(fieldNotAllowedFailure);
                document.fields.remove(pair.getKey());
            }
        }
        return document;
    }
}
