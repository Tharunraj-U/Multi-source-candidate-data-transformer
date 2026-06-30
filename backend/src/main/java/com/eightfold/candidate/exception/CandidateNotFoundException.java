package com.eightfold.candidate.exception;

import java.util.UUID;

public class CandidateNotFoundException extends RuntimeException {
    public CandidateNotFoundException(UUID id) {
        super("Candidate not found: " + id);
    }
}
