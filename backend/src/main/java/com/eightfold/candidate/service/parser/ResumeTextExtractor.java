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
    private static final Pattern PHONE_INTERNATIONAL = Pattern.compile(
            "(?:\\+\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)?\\d{3,4}[-.\\s]?\\d{4,6}");
    private static final Pattern PHONE_INDIA = Pattern.compile(
            "(?:\\+91|91)[-.\\s]?[6-9]\\d{9}|\\b[6-9]\\d{9}\\b");
    private static final Pattern YEAR_RANGE = Pattern.compile(
            "(\\d{4})\\s*[-–—]\\s*(\\d{4}|Present|Current)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLEX_DATE_RANGE = Pattern.compile(
            "(?i)((?:\\d{1,2}/)?\\d{4}|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s*\\d{4})"
                    + "\\s*[-–—to]+\\s*((?:\\d{1,2}/)?\\d{4}|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s*\\d{4}|present|current)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEGREE_LINE = Pattern.compile(
            "(?i)^(?:b\\.?\\s*tech|b\\.?\\s*e\\.?|b\\.?\\s*sc|bachelor|m\\.?\\s*tech|m\\.?\\s*sc|master|mba|ph\\.?\\s*d|b\\.?\\s*com|m\\.?\\s*com|diploma|associate)\\b");
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
        List<ParsedExperienceDTO> experience = filterExperience(extractExperience(lines));
        List<ParsedEducationDTO> education = filterEducation(extractEducation(lines));

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
            if (line.length() > 60 || EMAIL.matcher(line).find() || looksLikePhone(line)) {
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
        List<PhoneSpan> spans = new ArrayList<>();
        collectPhoneSpans(text, PHONE_INDIA, spans);
        collectPhoneSpans(text, PHONE_INTERNATIONAL, spans);
        spans = dedupeOverlappingSpans(spans);

        List<String> normalized = new ArrayList<>();
        for (PhoneSpan span : spans) {
            String formatted = normalizePhoneDigits(span.digits());
            if (formatted != null && !isDuplicateNumber(formatted, normalized)) {
                normalized.add(formatted);
            }
        }
        return normalized;
    }

    private static void collectPhoneSpans(String text, Pattern pattern, List<PhoneSpan> spans) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group().trim();
            String digits = raw.replaceAll("\\D", "");
            if (digits.length() >= 10 && digits.length() <= 15) {
                spans.add(new PhoneSpan(matcher.start(), matcher.end(), digits));
            }
        }
    }

    private static List<PhoneSpan> dedupeOverlappingSpans(List<PhoneSpan> spans) {
        spans.sort(Comparator.comparingInt(PhoneSpan::start));
        List<PhoneSpan> kept = new ArrayList<>();
        for (PhoneSpan span : spans) {
            boolean overlaps = kept.stream().anyMatch(k ->
                    span.start() < k.end() && span.end() > k.start());
            if (!overlaps) {
                kept.add(span);
            } else {
                PhoneSpan existing = kept.stream()
                        .filter(k -> span.start() < k.end() && span.end() > k.start())
                        .findFirst()
                        .orElse(null);
                if (existing != null && span.digits().length() > existing.digits().length()) {
                    kept.remove(existing);
                    kept.add(span);
                }
            }
        }
        return kept;
    }

    private static boolean isDuplicateNumber(String formatted, List<String> kept) {
        String digits = formatted.replaceAll("\\D", "");
        String core = lastTenDigits(digits);
        for (String other : kept) {
            String otherDigits = other.replaceAll("\\D", "");
            if (digits.equals(otherDigits)) {
                return true;
            }
            if (!core.isEmpty() && core.equals(lastTenDigits(otherDigits))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePhoneDigits(String digits) {
        if (digits.length() == 10 && digits.charAt(0) >= '6' && digits.charAt(0) <= '9') {
            return "+91" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }
        if (digits.length() >= 10) {
            return "+" + digits;
        }
        return null;
    }

    private static String lastTenDigits(String digits) {
        return digits.length() >= 10 ? digits.substring(digits.length() - 10) : digits;
    }

    private static boolean looksLikePhone(String line) {
        return PHONE_INDIA.matcher(line).find() || PHONE_INTERNATIONAL.matcher(line).find();
    }

    private record PhoneSpan(int start, int end, String digits) {}

    private static List<ParsedSkillDTO> extractSkills(String text) {
        String lower = text.toLowerCase();
        return SKILL_KEYWORDS.stream()
                .filter(lower::contains)
                .map(skill -> ParsedSkillDTO.builder().name(capitalizeSkill(skill)).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<ParsedExperienceDTO> filterExperience(List<ParsedExperienceDTO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().filter(ResumeTextExtractor::isValidExperience).toList();
    }

    public static List<ParsedEducationDTO> filterEducation(List<ParsedEducationDTO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().filter(ResumeTextExtractor::isValidEducation).toList();
    }

    private static boolean isValidExperience(ParsedExperienceDTO exp) {
        if (exp == null) {
            return false;
        }
        String company = exp.getCompany();
        String title = exp.getTitle();
        if (!isValidRoleLine(company) || !isValidRoleLine(title)) {
            return false;
        }
        if (isSectionHeader(company, "experience", "work history", "employment",
                "professional experience", "work experience", "employment history", "education", "skills")
                || isSectionHeader(title, "experience", "work history", "employment",
                "professional experience", "work experience", "employment history", "education", "skills")) {
            return false;
        }
        return exp.getStartDate() != null;
    }

    private static final Pattern INSTITUTION_KEYWORD = Pattern.compile(
            "(?i)\\b(university|college|institute|school|academy|polytechnic)\\b");

    private static boolean isValidEducation(ParsedEducationDTO edu) {
        if (edu == null) {
            return false;
        }
        String institution = edu.getInstitution();
        String degree = edu.getDegree();
        if (institution != null && isSectionHeader(institution, "education", "academic", "academics", "experience", "skills")) {
            return false;
        }
        if (institution != null && looksLikeSentenceNotSchool(institution)) {
            return false;
        }
        boolean hasInstitution = looksLikeInstitution(institution);
        boolean hasDegree = degree != null && !degree.isBlank() && !isBulletLine(degree);
        if (!hasInstitution && !hasDegree) {
            return false;
        }
        if (hasInstitution && !isValidRoleLine(institution)) {
            return false;
        }
        return true;
    }

    private static boolean looksLikeInstitution(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        return INSTITUTION_KEYWORD.matcher(line).find();
    }

    private static boolean looksLikeSentenceNotSchool(String line) {
        String lower = line.toLowerCase();
        if (line.endsWith(".") && !looksLikeInstitution(line)) {
            return true;
        }
        return lower.contains("solution") || lower.contains("automated")
                || lower.contains("addressing") || lower.contains("implemented")
                || lower.contains("developed") || lower.contains("delivered");
    }

    private static boolean isValidRoleLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        if (isBulletLine(line) || line.length() > 80) {
            return false;
        }
        String lower = line.toLowerCase();
        if (lower.startsWith("delivered ") || lower.startsWith("implemented ")
                || lower.startsWith("reviewed ") || lower.startsWith("developed ")
                || lower.startsWith("designed ") || lower.startsWith("collaborated ")
                || lower.startsWith("methodologies ")) {
            return false;
        }
        return true;
    }

    private static boolean isBulletLine(String line) {
        if (line == null || line.isBlank()) {
            return true;
        }
        String trimmed = line.strip();
        return trimmed.startsWith("·") || trimmed.startsWith("•") || trimmed.startsWith("*")
                || trimmed.startsWith("- ") || trimmed.startsWith("– ")
                || trimmed.matches("^\\d+\\s*[.)]\\s+.+");
    }

    private static String capitalizeSkill(String skill) {
        if ("node.js".equals(skill)) return "Node.js";
        if (skill.length() <= 3) return skill.toUpperCase();
        return Character.toUpperCase(skill.charAt(0)) + skill.substring(1);
    }

    private static List<ParsedExperienceDTO> extractExperience(List<String> lines) {
        List<ParsedExperienceDTO> sectionResults = extractExperienceInSection(lines);
        if (!sectionResults.isEmpty()) {
            return sectionResults;
        }
        return extractExperienceFromDateLines(lines);
    }

    private static List<ParsedExperienceDTO> extractExperienceInSection(List<String> lines) {
        List<ParsedExperienceDTO> results = new ArrayList<>();
        boolean inExperience = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSectionHeader(line, "experience", "work history", "employment",
                    "professional experience", "work experience", "employment history")) {
                inExperience = true;
                continue;
            }
            if (inExperience && isSectionHeader(line, "education", "skills", "projects", "certifications", "academic")) {
                break;
            }
            if (!inExperience) continue;

            ParsedExperienceDTO row = parseExperienceAtLine(lines, i);
            if (row != null) {
                results.add(row);
            }
        }
        return results;
    }

    private static List<ParsedExperienceDTO> extractExperienceFromDateLines(List<String> lines) {
        List<ParsedExperienceDTO> results = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (isSectionHeader(lines.get(i), "education", "skills", "projects", "certifications", "academic")) {
                break;
            }
            ParsedExperienceDTO row = parseExperienceAtLine(lines, i);
            if (row != null) {
                results.add(row);
            }
        }
        return results;
    }

    private static ParsedExperienceDTO parseExperienceAtLine(List<String> lines, int i) {
        String line = lines.get(i);
        if (isBulletLine(line)) {
            return null;
        }

        Matcher matcher = findDateRangeMatcher(line);
        if (matcher == null) {
            return null;
        }

        String startToken = matcher.group(1);
        String endToken = matcher.groupCount() >= 2 ? matcher.group(2) : null;
        LocalDate startDate = parseDateToken(startToken);
        if (startDate == null) {
            return null;
        }
        boolean current = endToken == null || isPresent(endToken);
        LocalDate endDate = current ? null : parseDateToken(endToken);

        String company = null;
        String title = null;
        for (int j = i - 1; j >= Math.max(0, i - 3); j--) {
            String candidate = lines.get(j);
            if (isBulletLine(candidate) || isSectionHeader(candidate, "experience", "work history", "employment",
                    "professional experience", "work experience", "employment history", "education", "skills")) {
                continue;
            }
            if (containsDateRange(candidate)) {
                break;
            }
            if (title == null) {
                title = candidate;
            } else if (company == null) {
                company = candidate;
                break;
            }
        }

        if (!isValidRoleLine(company) || !isValidRoleLine(title)) {
            return null;
        }

        return ParsedExperienceDTO.builder()
                .company(company)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .current(current)
                .build();
    }

    private static Matcher findDateRangeMatcher(String line) {
        Matcher yearMatcher = YEAR_RANGE.matcher(line);
        if (yearMatcher.find()) {
            return yearMatcher;
        }
        Matcher flexMatcher = FLEX_DATE_RANGE.matcher(line);
        if (flexMatcher.find()) {
            return flexMatcher;
        }
        return null;
    }

    private static boolean containsDateRange(String line) {
        return YEAR_RANGE.matcher(line).find() || FLEX_DATE_RANGE.matcher(line).find();
    }

    private static List<ParsedEducationDTO> extractEducation(List<String> lines) {
        List<ParsedEducationDTO> results = new ArrayList<>();
        boolean inEducation = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSectionHeader(line, "education", "academic", "academics")) {
                inEducation = true;
                continue;
            }
            if (inEducation && isSectionHeader(line, "experience", "skills", "projects", "certifications")) {
                break;
            }
            if (!inEducation) {
                continue;
            }
            if (isBulletLine(line) || isSectionHeader(line, "education", "academic", "academics")) {
                continue;
            }

            ParsedEducationDTO fromDateLine = parseEducationAtLine(lines, i);
            if (fromDateLine != null) {
                results.add(fromDateLine);
                continue;
            }

            if (looksLikeDegree(line)) {
                ParsedEducationDTO fromDegree = parseEducationFromDegreeLine(lines, i);
                if (fromDegree != null) {
                    results.add(fromDegree);
                }
            }
        }
        return results;
    }

    private static ParsedEducationDTO parseEducationAtLine(List<String> lines, int i) {
        String line = lines.get(i);
        Matcher matcher = findDateRangeMatcher(line);
        if (matcher == null) {
            return null;
        }

        String institution = null;
        String degree = null;
        for (int j = i - 1; j >= Math.max(0, i - 3); j--) {
            String candidate = lines.get(j);
            if (isBulletLine(candidate) || isSectionHeader(candidate, "education", "academic", "academics",
                    "experience", "skills", "projects")) {
                continue;
            }
            if (containsDateRange(candidate)) {
                break;
            }
            if (looksLikeDegree(candidate)) {
                degree = candidate;
            } else if (institution == null) {
                institution = candidate;
            }
        }

        if (degree == null && line.contains(",")) {
            degree = line.split(",")[0].trim();
        }

        LocalDate startDate = parseDateToken(matcher.group(1));
        LocalDate endDate = matcher.groupCount() >= 2 && matcher.group(2) != null
                ? parseDateToken(matcher.group(2)) : null;

        if (!isValidRoleLine(institution) && degree == null) {
            return null;
        }

        return ParsedEducationDTO.builder()
                .institution(institution)
                .degree(degree)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private static ParsedEducationDTO parseEducationFromDegreeLine(List<String> lines, int i) {
        String degree = lines.get(i);
        String institution = null;
        LocalDate startDate = null;
        LocalDate endDate = null;

        for (int j = i - 1; j >= Math.max(0, i - 2); j--) {
            String candidate = lines.get(j);
            if (!isBulletLine(candidate) && !looksLikeDegree(candidate)
                    && !isSectionHeader(candidate, "education", "academic", "academics")) {
                institution = candidate;
                break;
            }
        }

        for (int j = i + 1; j < Math.min(lines.size(), i + 3); j++) {
            Matcher matcher = findDateRangeMatcher(lines.get(j));
            if (matcher != null) {
                startDate = parseDateToken(matcher.group(1));
                endDate = matcher.groupCount() >= 2 && matcher.group(2) != null
                        ? parseDateToken(matcher.group(2)) : null;
                break;
            }
        }

        return ParsedEducationDTO.builder()
                .institution(institution)
                .degree(degree)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private static boolean looksLikeDegree(String line) {
        return line != null && DEGREE_LINE.matcher(line.strip()).find();
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
        String value = token.trim();

        try {
            if (value.matches("\\d{4}")) {
                return YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy")).atDay(1);
            }
            if (value.matches("(?i)(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s*\\d{4}")) {
                return YearMonth.parse(value.replaceAll("(?i)([a-z]+)\\.?", "$1 "),
                        DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)).atDay(1);
            }
            if (value.matches("\\d{1,2}/\\d{4}")) {
                String[] parts = value.split("/");
                return YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0])).atDay(1);
            }
            return LocalDate.parse(value + "-01-01");
        } catch (DateTimeParseException | NumberFormatException ex) {
            return null;
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
