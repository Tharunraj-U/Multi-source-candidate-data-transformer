package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.domain.model.ParsedEducationDTO;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResumeTextExtractorTest {

    @Test
    void extractsEducationFromSampleResumeText() throws Exception {
        String text = readResource("sample-resume.txt");
        UUID sourceId = UUID.randomUUID();

        ParsedCandidateDTO profile = ResumeTextExtractor.extract(text, sourceId);

        assertFalse(profile.getEducation().isEmpty());
        ParsedEducationDTO edu = profile.getEducation().stream()
                .filter(e -> e.getInstitution() != null && e.getInstitution().contains("Sri Eshwar"))
                .findFirst()
                .orElse(null);
        assertNotNull(edu);
        assertTrue(edu.getDegree() != null && edu.getDegree().contains("Bachelor"));
    }

    @Test
    void rejectsProjectDescriptionAsEducation() throws Exception {
        String text = readResource("sample-resume.txt");
        UUID sourceId = UUID.randomUUID();

        ParsedCandidateDTO profile = ResumeTextExtractor.extract(text, sourceId);

        assertTrue(profile.getEducation().stream()
                .noneMatch(e -> e.getInstitution() != null
                        && e.getInstitution().toLowerCase().contains("accessibility")));
    }

    private String readResource(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
