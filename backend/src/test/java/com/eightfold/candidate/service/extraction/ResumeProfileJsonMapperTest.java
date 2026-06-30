package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.domain.model.ParsedEducationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResumeProfileJsonMapperTest {

    private ResumeProfileJsonMapper mapper;
    private final UUID sourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mapper = new ResumeProfileJsonMapper(new ObjectMapper());
    }

    @Test
    void mapsGeminiJsonWithNormalizedEducation() throws Exception {
        String json = readResource("sample-gemini-response.json");

        ParsedCandidateDTO profile = mapper.fromJsonString(json, SourceType.RESUME, sourceId);

        assertEquals("Thangaraj M", profile.getFullName());
        assertEquals("Software Development Engineer II", profile.getHeadline());
        assertEquals(1, profile.getEducation().size());

        ParsedEducationDTO edu = profile.getEducation().getFirst();
        assertEquals("Sri Eshwar College of Engineering", edu.getInstitution());
        assertEquals("Bachelor of Engineering (Electronics and Communication Engineering)", edu.getDegree());
        assertEquals(LocalDate.of(2018, 1, 1), edu.getStartDate());
        assertEquals(LocalDate.of(2022, 1, 1), edu.getEndDate());
    }

    @Test
    void mapsExperienceWithDates() throws Exception {
        String json = readResource("sample-gemini-response.json");

        ParsedCandidateDTO profile = mapper.fromJsonString(json, SourceType.RESUME, sourceId);

        assertEquals(1, profile.getExperience().size());
        assertEquals("Amazon", profile.getExperience().getFirst().getCompany());
        assertEquals("Software Development Engineer II", profile.getExperience().getFirst().getTitle());
        assertEquals(LocalDate.of(2022, 1, 1), profile.getExperience().getFirst().getStartDate());
        assertTrue(profile.getExperience().getFirst().isCurrent());
    }

    @Test
    void normalizeEducationSplitsEmbeddedYearRange() {
        ParsedEducationDTO raw = ParsedEducationDTO.builder()
                .institution("Sri Eshwar College of Engineering, Coimbatore, IN 2018-2022")
                .degree("B.Tech")
                .build();

        ParsedEducationDTO normalized = mapper.normalizeEducation(raw);

        assertEquals("Sri Eshwar College of Engineering", normalized.getInstitution());
        assertEquals(LocalDate.of(2018, 1, 1), normalized.getStartDate());
        assertEquals(LocalDate.of(2022, 1, 1), normalized.getEndDate());
    }

    @Test
    void splitsCompoundSkillsFromJson() throws Exception {
        String json = """
                {
                  "skills": [
                    "Databases MySQL (relational); MongoDB, Redis, PocketBase (NoSQL)",
                    "React",
                    "DevOps & Tools, Docker, Git, GitHub"
                  ]
                }
                """;

        ParsedCandidateDTO profile = mapper.fromJsonString(json, SourceType.RESUME, sourceId);

        assertTrue(profile.getSkills().stream().anyMatch(s -> "MySQL".equals(s.getName())));
        assertTrue(profile.getSkills().stream().anyMatch(s -> "MongoDB".equals(s.getName())));
        assertTrue(profile.getSkills().stream().anyMatch(s -> "Docker".equals(s.getName())));
        assertFalse(profile.getSkills().stream().anyMatch(s -> s.getName().contains("Databases")));
    }

    @Test
    void rejectsProjectEntriesFromExperience() throws Exception {
        String json = """
                {
                  "experience": [
                    {
                      "company": "URL Shortener with Analytics",
                      "title": "Spring Boot, React, JWT",
                      "startDate": "2024-11",
                      "endDate": null,
                      "current": false
                    },
                    {
                      "company": "Fleet Studio",
                      "title": "Software Developer Intern",
                      "startDate": "2025-08",
                      "endDate": null,
                      "current": true
                    }
                  ]
                }
                """;

        ParsedCandidateDTO profile = mapper.fromJsonString(json, SourceType.RESUME, sourceId);

        assertEquals(1, profile.getExperience().size());
        assertEquals("Fleet Studio", profile.getExperience().getFirst().getCompany());
    }

    @Test
    void rejectsProjectSentenceAsInstitution() throws Exception {
        String json = """
                {
                  "education": [{
                    "institution": "automated solution for addressing website accessibility issues.",
                    "degree": null
                  }]
                }
                """;

        ParsedCandidateDTO profile = mapper.fromJsonString(json, SourceType.RESUME, sourceId);

        assertTrue(profile.getEducation().isEmpty());
    }

    private String readResource(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "Missing test resource: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
