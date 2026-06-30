package com.eightfold.candidate.service.normalize;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillSanitizerTest {

    @Test
    void splitsCategoryBlobIntoAtomicSkills() {
        List<String> raw = List.of(
                "Databases MySQL (relational); MongoDB, Redis, PocketBase (NoSQL)",
                "Distributed Systems, REST API design, multi-tier architecture, Redis caching, rate limiting, concurrency",
                "Security, OWASP Top 10, JWT/OAuth, input validation, RBAC, audit logging, secure coding",
                "DevOps & Tools, Docker, Git, GitHub, Linux, Maven, IntelliJ, VS Code");

        List<String> skills = SkillSanitizer.expandToAtomicSkills(raw);

        assertTrue(skills.contains("MySQL"));
        assertTrue(skills.contains("MongoDB"));
        assertTrue(skills.contains("Redis"));
        assertTrue(skills.contains("PocketBase"));
        assertTrue(skills.contains("Docker"));
        assertTrue(skills.contains("Git"));
        assertTrue(skills.contains("GitHub"));
        assertTrue(skills.contains("JWT"));
        assertTrue(skills.contains("OAuth"));
        assertTrue(skills.contains("RBAC"));
        assertFalse(skills.stream().anyMatch(s -> s.contains("Databases")));
        assertFalse(skills.stream().anyMatch(s -> s.contains("DevOps & Tools")));
    }

    @Test
    void keepsAlreadyAtomicSkills() {
        List<String> raw = List.of("React", "Python", "Spring Boot", "Node.js");

        List<String> skills = SkillSanitizer.expandToAtomicSkills(raw);

        assertEquals(List.of("React", "Python", "Spring Boot", "Node.js"), skills);
    }

    @Test
    void deduplicatesAcrossBlobs() {
        List<String> raw = List.of("Redis", "Redis caching", "Docker, Redis");

        List<String> skills = SkillSanitizer.expandToAtomicSkills(raw);

        assertEquals(1, skills.stream().filter(s -> s.equalsIgnoreCase("Redis")).count());
        assertTrue(skills.contains("Docker"));
    }
}
