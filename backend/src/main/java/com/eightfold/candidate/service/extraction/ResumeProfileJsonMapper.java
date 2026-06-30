package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.service.normalize.SkillSanitizer;
import com.eightfold.candidate.service.parser.ResumeContactSanitizer;
import com.eightfold.candidate.service.parser.ResumeTextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 4 of resume pipeline: LLM JSON response → ParsedCandidateDTO.
 */
@Component
public class ResumeProfileJsonMapper {

    private static final Pattern EMBEDDED_YEAR_RANGE = Pattern.compile(
            "(\\d{4})\\s*[-–—]\\s*(\\d{4})\\s*$");
    private static final Pattern INSTITUTION_KEYWORD = Pattern.compile(
            "(?i)\\b(university|college|institute|school|academy|polytechnic)\\b");

    private final ObjectMapper objectMapper;

    public ResumeProfileJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedCandidateDTO fromJsonString(String json, SourceType sourceType, UUID sourceId) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return fromJsonNode(node, sourceType, sourceId);
    }

    public ParsedCandidateDTO fromJsonNode(JsonNode node, SourceType sourceType, UUID sourceId) {
        ParsedCandidateDTO.ParsedCandidateDTOBuilder builder = ParsedCandidateDTO.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .fullName(text(node, "fullName"))
                .headline(text(node, "headline"))
                .location(text(node, "location"))
                .yearsExperience(decimal(node, "yearsExperience"));

        builder.emails(ResumeContactSanitizer.filterEmails(stringList(node, "emails")));
        builder.phones(stringList(node, "phones"));
        builder.skills(mapSkills(node.path("skills")));
        builder.links(mapLinks(node.path("links"), stringList(node, "emails")));
        builder.experience(ResumeTextExtractor.filterExperience(mapExperience(node.path("experience"))));
        builder.education(ResumeTextExtractor.filterEducation(mapEducation(node.path("education"))));
        return builder.build();
    }

    private List<ParsedLinkDTO> mapLinks(JsonNode linksNode, List<String> rawEmails) {
        List<ParsedLinkDTO> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (linksNode.isArray()) {
            for (JsonNode item : linksNode) {
                String url = item.isTextual() ? item.asText() : text(item, "url");
                if (url == null || url.isBlank()) {
                    continue;
                }
                LinkType type = parseLinkType(text(item, "type"), url);
                String normalized = normalizeLinkUrl(url.trim());
                if (seen.add(normalized.toLowerCase())) {
                    result.add(ParsedLinkDTO.builder().type(type).url(normalized).build());
                }
            }
        }
        for (ParsedLinkDTO recovered : ResumeContactSanitizer.linksFromMisplacedContacts(rawEmails, List.of())) {
            if (seen.add(recovered.getUrl().toLowerCase())) {
                result.add(recovered);
            }
        }
        return result;
    }

    private static LinkType parseLinkType(String raw, String url) {
        if (raw != null && !raw.isBlank()) {
            try {
                return LinkType.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return ResumeContactSanitizer.inferLinkType(url);
    }

    private static String normalizeLinkUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "https://" + url;
    }

    private List<ParsedSkillDTO> mapSkills(JsonNode skills) {
        List<String> rawNames = new ArrayList<>();
        if (skills.isArray()) {
            for (JsonNode skill : skills) {
                String name = skill.isTextual() ? skill.asText() : text(skill, "name");
                if (name != null && !name.isBlank()) {
                    rawNames.add(name.trim());
                }
            }
        }
        List<ParsedSkillDTO> result = new ArrayList<>();
        for (String name : SkillSanitizer.expandToAtomicSkills(rawNames)) {
            result.add(ParsedSkillDTO.builder().name(name).build());
        }
        return result;
    }

    private List<ParsedExperienceDTO> mapExperience(JsonNode items) {
        List<ParsedExperienceDTO> result = new ArrayList<>();
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode item : items) {
            result.add(ParsedExperienceDTO.builder()
                    .company(text(item, "company"))
                    .title(text(item, "title"))
                    .startDate(parseDate(text(item, "startDate")))
                    .endDate(parseDate(text(item, "endDate")))
                    .current(item.path("current").asBoolean(false))
                    .description(text(item, "description"))
                    .build());
        }
        return result;
    }

    private List<ParsedEducationDTO> mapEducation(JsonNode items) {
        List<ParsedEducationDTO> result = new ArrayList<>();
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode item : items) {
            result.add(normalizeEducation(ParsedEducationDTO.builder()
                    .institution(text(item, "institution"))
                    .degree(text(item, "degree"))
                    .fieldOfStudy(text(item, "fieldOfStudy"))
                    .startDate(parseDate(text(item, "startDate")))
                    .endDate(parseDate(text(item, "endDate")))
                    .build()));
        }
        return result;
    }

    ParsedEducationDTO normalizeEducation(ParsedEducationDTO edu) {
        if (edu == null) {
            return null;
        }
        String institution = edu.getInstitution();
        LocalDate startDate = edu.getStartDate();
        LocalDate endDate = edu.getEndDate();

        if (institution != null) {
            Matcher matcher = EMBEDDED_YEAR_RANGE.matcher(institution);
            if (matcher.find()) {
                if (startDate == null) {
                    startDate = yearStart(matcher.group(1));
                }
                if (endDate == null) {
                    endDate = yearStart(matcher.group(2));
                }
                institution = institution.substring(0, matcher.start()).replaceAll("[,\\s]+$", "").trim();
            }
            institution = stripLocationSuffix(institution);
        }

        return ParsedEducationDTO.builder()
                .institution(institution)
                .degree(edu.getDegree())
                .fieldOfStudy(edu.getFieldOfStudy())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private String stripLocationSuffix(String institution) {
        if (institution == null) {
            return null;
        }
        int comma = institution.indexOf(',');
        if (comma > 0 && INSTITUTION_KEYWORD.matcher(institution.substring(0, comma)).find()) {
            return institution.substring(0, comma).trim();
        }
        return institution;
    }

    private LocalDate yearStart(String year) {
        try {
            return LocalDate.of(Integer.parseInt(year), 1, 1);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value.trim();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.get(field).asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<String> stringList(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        JsonNode array = node.path(field);
        if (!array.isArray()) {
            return values;
        }
        for (JsonNode item : array) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if ("present".equalsIgnoreCase(value) || "current".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            if (value.matches("\\d{4}")) {
                return LocalDate.of(Integer.parseInt(value), 1, 1);
            }
            if (value.matches("\\d{4}-\\d{2}")) {
                return YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NumberFormatException ex) {
            return null;
        }
    }
}
