/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its
 * affiliates and licensors ("Micro Focus") are set forth in the express
 * warranty statements accompanying such products and services. Nothing
 * herein should be construed as constituting an additional warranty.
 * Micro Focus shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated
 * otherwise, a valid license is required for possession, use or copying.
 * Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial
 * Items are licensed to the U.S. Government under vendor's standard
 * commercial license.
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
