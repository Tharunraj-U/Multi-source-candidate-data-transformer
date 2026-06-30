package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.model.*;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class ResumeTextExtractor {

    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+?1[-.\\s]?)?(?:\\(?\\d{3}\\)?[-.\\s]?)?\\d{3}[-.\\s]?\\d{4}");
    private static final Pattern YEAR_RANGE = Pattern.compile(
            "(\\d{4}|Present|Current)\\s*[-–—]\\s*(\\d{4}|Present|Current)?", Pattern.CASE_INSENSITIVE);
    private static final Set<String> SKILL_KEYWORDS = Set.of(
            "java", "python", "javascript", "typescript", "react", "spring", "sql", "aws",
            "kubernetes", "docker", "node.js", "angular", "vue", "c#", "c++", "go", "rust",
            "mongodb", "postgresql", "redis", "kafka", "graphql", "rest", "api", "git");

    public static ParsedCandidateDTO extract(String text, UUID sourceId) {
        String normalized = text.replace("\r", "").trim();
        List<String> lines = Arrays.stream(normalized.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        String fullName = inferName(lines);
        List<String> emails = extractEmails(normalized);
        List<String> phones = extractPhones(normalized);
        List<ParsedSkillDTO> skills = extractSkills(normalized);
        List<ParsedExperienceDTO> experience = extractExperience(lines);
        List<ParsedEducationDTO> education = extractEducation(lines);

        return ParsedCandidateDTO.builder()
                .sourceId(sourceId)
                .fullName(fullName)
                .emails(emails)
                .phones(phones)
                .skills(skills)
                .experience(experience)
                .education(education)
                .yearsExperience(estimateYearsExperience(experience))
                .build();
    }

    private static String inferName(List<String> lines) {
        for (String line : lines.stream().limit(5).toList()) {
            if (line.length() > 60 || EMAIL.matcher(line).find() || PHONE.matcher(line).find()) {
                continue;
            }
            if (line.split("\\s+").length >= 2 && line.split("\\s+").length <= 5) {
                return line;
            }
        }
        return lines.isEmpty() ? null : lines.getFirst();
    }

    private static List<String> extractEmails(String text) {
        return EMAIL.matcher(text).results()
                .map(m -> m.group().toLowerCase())
                .distinct()
                .toList();
    }

    private static List<String> extractPhones(String text) {
        return PHONE.matcher(text).results()
                .map(m -> m.group())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static List<ParsedSkillDTO> extractSkills(String text) {
        String lower = text.toLowerCase();
        return SKILL_KEYWORDS.stream()
                .filter(lower::contains)
                .map(skill -> ParsedSkillDTO.builder().name(capitalizeSkill(skill)).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String capitalizeSkill(String skill) {
        if ("node.js".equals(skill)) return "Node.js";
        if (skill.length() <= 3) return skill.toUpperCase();
        return Character.toUpperCase(skill.charAt(0)) + skill.substring(1);
    }

    private static List<ParsedExperienceDTO> extractExperience(List<String> lines) {
        List<ParsedExperienceDTO> results = new ArrayList<>();
        boolean inExperience = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSectionHeader(line, "experience", "work history", "employment")) {
                inExperience = true;
                continue;
            }
            if (inExperience && isSectionHeader(line, "education", "skills", "projects", "certifications")) {
                break;
            }
            if (!inExperience) continue;

            Matcher dateMatcher = YEAR_RANGE.matcher(line);
            if (dateMatcher.find()) {
                String titleOrCompany = i > 0 ? lines.get(i - 1) : null;
                String company = titleOrCompany;
                String title = null;
                if (i > 1 && !YEAR_RANGE.matcher(lines.get(i - 2)).find()) {
                    title = lines.get(i - 2);
                }
                LocalDate start = parseDateToken(dateMatcher.group(1));
                String endToken = dateMatcher.group(2);
                boolean current = endToken == null || isPresent(endToken);
                LocalDate end = current ? null : parseDateToken(endToken);

                results.add(ParsedExperienceDTO.builder()
                        .company(company != null ? company : "Unknown")
                        .title(title)
                        .startDate(start)
                        .endDate(end)
                        .current(current)
                        .build());
            }
        }
        return results;
    }

    private static List<ParsedEducationDTO> extractEducation(List<String> lines) {
        List<ParsedEducationDTO> results = new ArrayList<>();
        boolean inEducation = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSectionHeader(line, "education")) {
                inEducation = true;
                continue;
            }
            if (inEducation && isSectionHeader(line, "experience", "skills", "projects")) {
                break;
            }
            if (!inEducation) continue;

            Matcher dateMatcher = YEAR_RANGE.matcher(line);
            if (dateMatcher.find() && i > 0) {
                results.add(ParsedEducationDTO.builder()
                        .institution(lines.get(i - 1))
                        .degree(line.contains(",") ? line.split(",")[0].trim() : null)
                        .startDate(parseDateToken(dateMatcher.group(1)))
                        .endDate(dateMatcher.group(2) != null ? parseDateToken(dateMatcher.group(2)) : null)
                        .build());
            }
        }
        return results;
    }

    private static boolean isSectionHeader(String line, String... keywords) {
        String normalized = line.toLowerCase().replaceAll("[^a-z ]", "").trim();
        for (String keyword : keywords) {
            if (normalized.equals(keyword) || normalized.startsWith(keyword + " ")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPresent(String token) {
        return token != null && token.matches("(?i)present|current");
    }

    private static LocalDate parseDateToken(String token) {
        if (token == null || isPresent(token)) return null;
        try {
            return YearMonth.parse(token, DateTimeFormatter.ofPattern("yyyy")).atDay(1);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(token + "-01-01");
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static BigDecimal estimateYearsExperience(List<ParsedExperienceDTO> experience) {
        if (experience.isEmpty()) return null;
        LocalDate earliest = experience.stream()
                .map(ParsedExperienceDTO::getStartDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
        if (earliest == null) return null;
        long months = java.time.temporal.ChronoUnit.MONTHS.between(earliest, LocalDate.now());
        return BigDecimal.valueOf(months / 12.0).setScale(1, java.math.RoundingMode.HALF_UP);
    }
}
