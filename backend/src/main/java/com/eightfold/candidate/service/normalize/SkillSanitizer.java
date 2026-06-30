package com.eightfold.candidate.service.normalize;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Splits compound / category skill strings from LLM output into atomic skill names.
 */
public final class SkillSanitizer {

    private static final Pattern SPLIT_DELIMITERS = Pattern.compile("[,;/]|\\s+and\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATEGORY_PREFIX = Pattern.compile(
            "(?i)^(Databases|Security|Testing\\s*&\\s*Quality|DevOps\\s*&\\s*Tools|Languages?)\\s*");
    private static final Pattern PARENS = Pattern.compile("\\([^)]*\\)");
    private static final int MAX_SKILL_LENGTH = 40;
    private static final int MAX_WORDS = 4;

    private static final List<String> KNOWN_SKILLS = List.of(
            "Spring Data JPA", "Spring Validation", "Spring Security", "Spring Boot",
            "OWASP Top 10", "REST API", "Node.js", "React.js", "VS Code", "IntelliJ IDEA",
            "PocketBase", "PostgreSQL", "MongoDB", "MySQL", "JavaScript", "TypeScript",
            "Kubernetes", "Java", "Python", "React", "Redis", "Docker", "AWS", "SQL",
            "HTML", "CSS", "Git", "GitHub", "Linux", "Maven", "JUnit", "Postman", "JMeter",
            "JWT", "OAuth", "Thymeleaf", "Spring", "Go", "GraphQL", "Kafka", "Angular",
            "Vue", "C#", "C++", "Rust", "RBAC", "Tor", "API");

    private SkillSanitizer() {}

    public static List<String> expandToAtomicSkills(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : raw) {
            if (item != null && !item.isBlank()) {
                expandOne(item.trim(), result);
            }
        }
        return List.copyOf(result);
    }

    private static void expandOne(String value, Set<String> out) {
        String withoutCategory = CATEGORY_PREFIX.matcher(value).replaceFirst("").trim();
        if (containsDelimiters(withoutCategory)) {
            for (String part : SPLIT_DELIMITERS.split(withoutCategory)) {
                addPart(part, out);
            }
            extractKnownFromBlob(value).forEach(skill -> out.add(skill));
            return;
        }
        addPart(withoutCategory, out);
    }

    private static void addPart(String part, Set<String> out) {
        if (part == null || part.isBlank()) {
            return;
        }
        String cleaned = CATEGORY_PREFIX.matcher(part).replaceFirst("").trim();
        cleaned = PARENS.matcher(cleaned).replaceAll("").trim();
        if (cleaned.isBlank()) {
            return;
        }
        if (isValidAtomicSkill(cleaned)) {
            out.add(cleaned);
            return;
        }
        if (containsDelimiters(cleaned)) {
            for (String sub : SPLIT_DELIMITERS.split(cleaned)) {
                addPart(sub, out);
            }
            return;
        }
        List<String> extracted = extractKnownFromBlob(cleaned);
        if (!extracted.isEmpty()) {
            out.addAll(extracted);
        } else if (cleaned.length() <= MAX_SKILL_LENGTH && cleaned.split("\\s+").length <= MAX_WORDS) {
            out.add(cleaned);
        }
    }

    private static boolean containsDelimiters(String value) {
        return value.contains(",") || value.contains(";") || value.contains("/")
                || value.toLowerCase(Locale.ROOT).contains(" and ");
    }

    private static boolean isValidAtomicSkill(String skill) {
        if (skill.length() > MAX_SKILL_LENGTH) {
            return false;
        }
        if (skill.split("\\s+").length > MAX_WORDS) {
            return false;
        }
        return !CATEGORY_PREFIX.matcher(skill).find();
    }

    private static List<String> extractKnownFromBlob(String blob) {
        String lower = blob.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (String skill : KNOWN_SKILLS) {
            if (containsSkillToken(lower, skill.toLowerCase(Locale.ROOT))) {
                found.add(skill);
            }
        }
        return found;
    }

    private static boolean containsSkillToken(String haystackLower, String skillLower) {
        int idx = haystackLower.indexOf(skillLower);
        while (idx >= 0) {
            boolean startOk = idx == 0 || !Character.isLetterOrDigit(haystackLower.charAt(idx - 1));
            int end = idx + skillLower.length();
            boolean endOk = end >= haystackLower.length() || !Character.isLetterOrDigit(haystackLower.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            idx = haystackLower.indexOf(skillLower, idx + 1);
        }
        return false;
    }
}
