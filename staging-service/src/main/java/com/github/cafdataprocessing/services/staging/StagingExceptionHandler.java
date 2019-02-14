/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.services.staging;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.github.cafdataprocessing.services.staging.exceptions.StagingException;
import com.github.cafdataprocessing.services.staging.models.BatchError;

@ControllerAdvice
public class StagingExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(StagingException.class)
    public final ResponseEntity<Object> handleStagingException(Exception ex, WebRequest request) {
        final BatchError err = new BatchError();
        err.setCode(HttpStatus.INTERNAL_SERVER_ERROR.name());
        err.setMessgae(ex.getMessage());
        return buildResponseEntity(err);
    }

    private ResponseEntity<Object> buildResponseEntity(final BatchError batchError) {
        return new ResponseEntity<>(batchError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
