package com.eightfold.candidate.service.normalize;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class EmailValidationService {

    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public String validate(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        return EMAIL.matcher(email.trim()).matches() ? "valid" : "invalid";
    }
}
