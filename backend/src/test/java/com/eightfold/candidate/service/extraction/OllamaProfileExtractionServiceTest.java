package com.eightfold.candidate.service.extraction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaProfileExtractionServiceTest {

    @Test
    void unwrapJsonStripsMarkdownFence() {
        String raw = """
                ```json
                {"fullName": "Thangaraj M"}
                ```
                """;
        assertEquals("{\"fullName\": \"Thangaraj M\"}", OllamaProfileExtractionService.unwrapJson(raw));
    }

    @Test
    void unwrapJsonPassesThroughPlainJson() {
        String raw = "{\"fullName\": \"Test\"}";
        assertEquals(raw, OllamaProfileExtractionService.unwrapJson(raw));
    }
}
