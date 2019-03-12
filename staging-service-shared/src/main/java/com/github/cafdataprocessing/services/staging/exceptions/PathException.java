package com.github.cafdataprocessing.services.staging.exceptions;

public class PathException extends Exception {
    public PathException(final String path){
        super(path);
    }
}
