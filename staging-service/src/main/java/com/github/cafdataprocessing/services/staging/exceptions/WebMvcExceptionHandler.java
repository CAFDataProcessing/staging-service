package com.github.cafdataprocessing.services.staging.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class WebMvcExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(WebMvcHandledRuntimeException.class)
    public final ResponseEntity<Object> handleStagingException(Exception ex, WebRequest webRequest) {
        return new ResponseEntity<>(ex.getMessage(), ((WebMvcHandledRuntimeException)ex).getStatus());
    }
}