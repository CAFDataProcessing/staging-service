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

import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FieldValidator
{
    final Set<String> allowedFields;
    final Map<String, Set<String>> allowedFlattenedFields;

    public FieldValidator(final String validationFile) throws ValidationFileAdapterException
    {
        ValidationFileAdapter adapter = new ValidationFileAdapter(validationFile);
        allowedFields = adapter.getFieldKeys();
        allowedFlattenedFields = adapter.getFlattenedFieldKeys();
    }

    public DocumentWorkerDocument validate(final DocumentWorkerDocument document)
    {
        final Set<String> keySet = new HashSet<>(document.fields.keySet());

        for (final String key : keySet) {
            if (!(isValidField(key) || isValidFlattenedField(key))) {
                final DocumentWorkerFailure fieldNotAllowedFailure = new DocumentWorkerFailure();
                fieldNotAllowedFailure.failureId = "FIELD-NOT-ALLOWED-FAILURE";
                fieldNotAllowedFailure.failureMessage
                    = String.format(key + " is not allowed to be set by the agent");
                document.failures.add(fieldNotAllowedFailure);
                document.fields.remove(key);
            }
        }
        return document;
    }

    private boolean isValidField(final String fieldKey)
    {
        for (final String allowedField : allowedFields) {
            if (fieldKey.equals(allowedField)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidFlattenedField(final String fieldKey)
    {
        for (final String flattenedPrefix : allowedFlattenedFields.keySet()) {
            final Set<String> flattenedSuffixes = allowedFlattenedFields.get(flattenedPrefix);
            for (final String flattenedSuffix : flattenedSuffixes) {
                if (fieldKey.startsWith(flattenedPrefix) && fieldKey.endsWith(flattenedSuffix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
