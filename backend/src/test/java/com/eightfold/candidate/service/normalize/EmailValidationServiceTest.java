package com.eightfold.candidate.service.normalize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailValidationServiceTest {

    private final EmailValidationService service = new EmailValidationService();

    @Test
    void acceptsWellFormedEmail() {
        assertEquals("valid", service.validate("thangaraj@example.com"));
    }

    @Test
    void rejectsMalformedEmail() {
        assertEquals("invalid", service.validate("not-an-email"));
    }

    @Test
    void returnsUnknownForBlank() {
        assertEquals("unknown", service.validate("  "));
        assertEquals("unknown", service.validate(null));
    }
}
