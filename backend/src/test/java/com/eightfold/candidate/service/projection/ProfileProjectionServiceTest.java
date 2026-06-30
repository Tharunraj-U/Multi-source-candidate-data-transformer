package com.eightfold.candidate.service.projection;

import com.eightfold.candidate.dto.response.ApiDtos;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileProjectionServiceTest {

    private final ProfileProjectionService service =
            new ProfileProjectionService(new RuntimeConfigValidator());

    @Test
    void projectsMappedFieldsAndConfidence() {
        ApiDtos.CandidateResponseDto profile = ApiDtos.CandidateResponseDto.builder()
                .candidateId(UUID.randomUUID())
                .fullName("Jane Doe")
                .emails(List.of(ApiDtos.EmailDto.builder()
                        .address("jane@example.com")
                        .validationStatus("valid")
                        .build()))
                .confidence(Map.of("full_name", new BigDecimal("0.95")))
                .build();

        String config = """
                {
                  "fields": [
                    { "path": "candidateName", "from": "full_name" },
                    { "path": "primaryEmail", "from": "emails[0]" }
                  ],
                  "includeConfidence": true,
                  "missing": "omit"
                }
                """;

        Map<String, Object> projected = service.project(profile, config);

        assertEquals("Jane Doe", projected.get("candidateName"));
        assertEquals("jane@example.com", projected.get("primaryEmail"));
        assertTrue(projected.containsKey("confidence"));
    }

    @Test
    void rejectsInvalidConfig() {
        assertThrows(com.eightfold.candidate.exception.ValidationException.class,
                () -> service.parseConfig("{"));
    }

    @Test
    void rejectsUnknownFromPath() {
        assertThrows(com.eightfold.candidate.exception.ProjectionException.class,
                () -> CanonicalPathResolver.validatePath("unknown_field"));
    }
}
