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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.worker.document.DocumentWorkerDocument;
import com.hpe.caf.worker.document.DocumentWorkerFailure;
import com.hpe.caf.worker.document.DocumentWorkerFieldValue;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public final class FieldValidator implements FieldValidatorInterface
{
    private final List<Pattern> allowedFieldPatterns;

    public FieldValidator(final String validationFile) throws IOException
    {
        final ValidationFileAdapter adapter = new ValidationFileAdapter(validationFile);
        allowedFieldPatterns = adapter.getFieldKeys().stream().map(Pattern::compile).collect(Collectors.toList());
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
                try {
                    if (!isValidField(key)) {
                        final DocumentWorkerFailure fieldNotAllowedFailure = new DocumentWorkerFailure();
                        fieldNotAllowedFailure.failureId = "IW-001";
                        fieldNotAllowedFailure.failureMessage = key + " is not allowed to be set by the agent";

                        ObjectMapper mapper = new ObjectMapper();
                        fieldNotAllowedFailure.failureStack = "Invalid Field Value: "
                            + mapper.writeValueAsString(document.fields.get(key));

                        try {
                            document.failures.add(fieldNotAllowedFailure);
                        } catch (UnsupportedOperationException ex) {
                            List<DocumentWorkerFailure> writableFailureList = new ArrayList<>(document.failures);
                            writableFailureList.add(fieldNotAllowedFailure);
                            document.failures = new ArrayList<>(writableFailureList);
                        }

                        try {
                            document.fields.remove(key);
                        } catch (UnsupportedOperationException ex) {
                            Map<String, List<DocumentWorkerFieldValue>> writableFields = new HashMap<>(document.fields);
                            writableFields.remove(key);
                            document.fields = new HashMap<>(writableFields);
                        }
                    }
                } catch (final JsonProcessingException ex) {
                    log.error("Unable to write invalid field value as string", ex);
                }
            }

            if (document.subdocuments != null) {
                final ArrayList<DocumentWorkerDocument> subDocs = new ArrayList<>(document.subdocuments);
                subDocs.replaceAll(this::validate);

                document.subdocuments = subDocs;
            }
        }

        return document;
    }

    private boolean isValidField(final String fieldKey)
    {
        return allowedFieldPatterns
            .stream()
            .anyMatch(allowedFieldPattern -> allowedFieldPattern.matcher(fieldKey).matches());
    }
}
