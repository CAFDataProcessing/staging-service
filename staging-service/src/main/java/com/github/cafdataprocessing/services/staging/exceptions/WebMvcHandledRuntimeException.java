package com.github.cafdataprocessing.services.staging.exceptions;

import org.springframework.http.HttpStatus;

public class WebMvcHandledRuntimeException extends RuntimeException {
    private HttpStatus status;

    public WebMvcHandledRuntimeException(HttpStatus status, String message){
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
