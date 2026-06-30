package com.eightfold.candidate.exception;

import lombok.Getter;

@Getter
public class SourceParseException extends RuntimeException {

    private final String errorCode;

    public SourceParseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SourceParseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
