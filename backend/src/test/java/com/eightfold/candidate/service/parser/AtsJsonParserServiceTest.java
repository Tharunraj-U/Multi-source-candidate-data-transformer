package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AtsJsonParserServiceTest {

    private final AtsJsonParserService parser = new AtsJsonParserService();

    @Test
    void parsesRichAtsPayload(@TempDir Path tempDir) throws Exception {
        String json = Files.readString(Path.of("src/test/resources/sample-ats.json"));
        Path file = tempDir.resolve("ats.json");
        Files.writeString(file, json);

        RawSource source = RawSource.builder()
                .sourceId(UUID.randomUUID())
                .sourceType(SourceType.ATS_JSON)
                .storagePath(file.toString())
                .build();

        var result = parser.parse(source);

        assertEquals("John Michael Doe", result.getFullName());
        assertEquals("Senior Java Full Stack Developer", result.getHeadline());
        assertEquals("San Francisco, California, USA", result.getLocation());
        assertEquals(2, result.getEmails().size());
        assertEquals(1, result.getPhones().size());
        assertEquals(14, result.getSkills().size());
        assertEquals(2, result.getExperience().size());
        assertEquals(1, result.getEducation().size());
        assertEquals(3, result.getLinks().size());
        assertEquals(0, new java.math.BigDecimal("8.5").compareTo(result.getYearsExperience()));
        assertTrue(result.getExperience().getFirst().isCurrent());
        assertEquals("Google", result.getExperience().getFirst().getCompany());
    }
}
