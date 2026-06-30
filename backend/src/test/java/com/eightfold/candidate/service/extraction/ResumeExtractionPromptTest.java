package com.eightfold.candidate.service.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResumeExtractionPromptTest {

    @Test
    void buildIncludesResumeTextAndSchema() {
        String resumeText = "Thangaraj M\nSoftware Development Engineer II";
        String prompt = ResumeExtractionPrompt.build(resumeText);

        assertTrue(prompt.contains("resume parser"));
        assertTrue(prompt.contains("education"));
        assertTrue(prompt.contains("links"));
        assertTrue(prompt.contains("NEVER put URLs in emails"));
        assertTrue(prompt.contains("SKILLS (critical)"));
        assertTrue(prompt.contains("ONE skill per array element"));
        assertTrue(prompt.contains("--- RESUME TEXT ---"));
        assertTrue(prompt.contains(resumeText));
    }

    @Test
    void truncateKeepsHeadAndTailForLongText() {
        String longText = "A".repeat(8000);
        String truncated = ResumeExtractionPrompt.truncate(longText, 6000);

        assertTrue(truncated.length() < longText.length());
        assertTrue(truncated.contains("...[truncated]..."));
    }
}
