package com.eightfold.candidate.exception;

import lombok.Getter;

@Getter
public class ProjectionException extends RuntimeException {

    private final String fieldPath;

    public ProjectionException(String fieldPath, String message) {
        super(message);
        this.fieldPath = fieldPath;
    }
}
