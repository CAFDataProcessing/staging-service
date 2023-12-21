/*
 * Copyright 2019-2024 Open Text.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FieldValidator implements FieldValidatorInterface
{
    private final List<Pattern> allowedFieldPatterns;
    private final ObjectMapper mapper = new ObjectMapper();

    public FieldValidator(final String validationFile) throws IOException
    {
        allowedFieldPatterns = ValidationFileAdapter.getFieldNamePatterns(validationFile);
    }

    @Override
    public DocumentWorkerDocument validate(final DocumentWorkerDocument document)
    {
        if (document.fields != null) {
            final Set<String> keySet = new HashSet<>(document.fields.keySet());

            if (document.failures == null) {
                document.failures = new ArrayList<>();
            }

            for (final String key : keySet) {
                if (!isValidField(key)) {
                    final DocumentWorkerFailure fieldNotAllowedFailure = new DocumentWorkerFailure();
                    fieldNotAllowedFailure.failureId = "IW-001";
                    fieldNotAllowedFailure.failureMessage = key + " is not allowed to be set by the agent.  Value sent: "
                        + tryGetFieldValue(document, key);

                    try {
                        document.failures.add(fieldNotAllowedFailure);
                    } catch (final UnsupportedOperationException ex) {
                        final List<DocumentWorkerFailure> writableFailureList = new ArrayList<>(document.failures);
                        writableFailureList.add(fieldNotAllowedFailure);
                        document.failures = writableFailureList;
                    }

                    try {
                        document.fields.remove(key);
                    } catch (final UnsupportedOperationException ex) {
                        final Map<String, List<DocumentWorkerFieldValue>> writableFields = new HashMap<>(document.fields);
                        writableFields.remove(key);
                        document.fields = writableFields;
                    }
                }
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
        return allowedFieldPatterns
            .stream()
            .anyMatch(allowedFieldPattern -> allowedFieldPattern.matcher(fieldKey).matches());
    }

    private String tryGetFieldValue(final DocumentWorkerDocument document, final String key)
    {
        try {
            return mapper.writeValueAsString(document.fields.get(key));
        } catch (final JsonProcessingException ex) {
            log.error("Unable to write invalid field value as string", ex);
            return "<Failed to get value>";
        }
    }
}
