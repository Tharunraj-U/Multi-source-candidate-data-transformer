package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live test against Ollama Docker — skipped when Ollama is not running.
 * Run: docker compose up -d && mvn test -Dtest=OllamaIntegrationTest
 */
class OllamaIntegrationTest {

    @Test
    void extractsProfileFromSampleResumeText() throws Exception {
        assumeTrue(isOllamaReachable(), "Ollama not running — start with: docker compose up -d");

        String resumeText = readResource("sample-resume.txt");
        OllamaProfileExtractionService service = new OllamaProfileExtractionService(
                new ObjectMapper(),
                new ResumeProfileJsonMapper(new ObjectMapper()),
                "http://localhost:11434/v1",
                System.getenv().getOrDefault("OLLAMA_MODEL", "llama3:latest"),
                true,
                6000,
                2048);

        ParsedCandidateDTO result = service.extractFromResumeText(
                resumeText, UUID.randomUUID(), null);

        assertNotNull(result.getFullName());
        assertTrue(result.getFullName().toLowerCase().contains("thangaraj")
                || result.getEmails() != null && !result.getEmails().isEmpty()
                || !result.getEducation().isEmpty()
                || !result.getExperience().isEmpty(),
                "Expected at least one meaningful field from Ollama extraction");
    }

    private static boolean isOllamaReachable() {
        try {
            RestClient.create("http://localhost:11434")
                    .get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String readResource(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "Missing: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
