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

import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class FieldValidator implements FieldValidatorInterface
{
    private final ArrayList<String> allowedFields;

    public FieldValidator(final String validationFile) throws IOException
    {
        final ValidationFileAdapter adapter = new ValidationFileAdapter(validationFile);
        allowedFields = adapter.getFieldKeys();
    }

    @Override
    public DocumentWorkerDocument validate(final DocumentWorkerDocument document)
    {
        final Set<String> keySet = new HashSet<>(document.fields.keySet());

        if (document.failures == null) {
            document.failures = new ArrayList<>();
        }

        for (final String key : keySet) {
            try {
                if (!isValidField(key)) {
                    final DocumentWorkerFailure fieldNotAllowedFailure = new DocumentWorkerFailure();
                    fieldNotAllowedFailure.failureId = "IW-001";
                    fieldNotAllowedFailure.failureMessage = key + " is not allowed to be set by the agent";
                    document.failures.add(fieldNotAllowedFailure);
                    document.fields.remove(key);
                }
            } catch (final UnsupportedOperationException ex) {
                throw new UnsupportedOperationException("Error while modifying immutable Collection", ex);
            }
        }

        if (document.subdocuments != null) {
            final ArrayList<DocumentWorkerDocument> subDocs = new ArrayList<>(document.subdocuments);
            subDocs.replaceAll(this::validate);

            document.subdocuments = subDocs;
        }

        return document;
    }

    private boolean isValidField(final String fieldKey)
    {
        for (final String field : allowedFields) {
            if (fieldKey.matches(field)) {
                return true;
            }
        }
        return false;
    }
}
