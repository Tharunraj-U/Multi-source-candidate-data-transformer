package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.exception.SourceParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class AtsJsonParserService implements SourceParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.ATS_JSON;
    }

    @Override
    public ParsedCandidateDTO parse(RawSource source) {
        if (source.getStoragePath() == null) {
            throw new SourceParseException("ATS_MISSING_FILE", "ATS JSON storage path is required");
        }
        try {
            JsonNode root = objectMapper.readTree(Path.of(source.getStoragePath()).toFile());
            return ParsedCandidateDTO.builder()
                    .sourceType(SourceType.ATS_JSON)
                    .sourceId(source.getSourceId())
                    .fullName(readName(root))
                    .headline(text(root, "headline"))
                    .about(firstText(root, "summary", "about"))
                    .location(readLocation(root.get("location")))
                    .yearsExperience(readYearsExperience(root))
                    .emails(readStringList(root.get("emails")))
                    .phones(readStringList(root.get("phones")))
                    .skills(readSkills(root.get("skills")))
                    .experience(readExperience(root.get("experience")))
                    .education(readEducation(root.get("education")))
                    .links(readLinks(root))
                    .build();
        } catch (SourceParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceParseException("ATS_PARSE_ERROR", "Failed to parse ATS JSON: " + ex.getMessage(), ex);
        }
    }

    private static String readName(JsonNode root) {
        return firstText(root, "fullName", "full_name", "name");
    }

    private static String firstText(JsonNode root, String... fields) {
        for (String field : fields) {
            String value = text(root, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value.isTextual()) {
            String trimmed = value.asText().trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return null;
    }

    private static String readLocation(JsonNode location) {
        if (location == null || location.isNull()) {
            return null;
        }
        if (location.isTextual()) {
            String value = location.asText().trim();
            return value.isEmpty() ? null : value;
        }
        if (!location.isObject()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String field : List.of("city", "state", "country")) {
            String value = text(location, field);
            if (value != null) {
                parts.add(value);
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static BigDecimal readYearsExperience(JsonNode root) {
        for (String field : List.of("totalExperienceYears", "yearsExperience", "years_experience")) {
            if (!root.hasNonNull(field)) {
                continue;
            }
            JsonNode value = root.get(field);
            if (value.isNumber()) {
                return BigDecimal.valueOf(value.asDouble());
            }
            if (value.isTextual()) {
                try {
                    return new BigDecimal(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // try next field
                }
            }
        }
        return null;
    }

    private static List<String> readStringList(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        array.forEach(item -> {
            if (item.isTextual()) {
                String value = item.asText().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        });
        return values;
    }

    private static List<ParsedSkillDTO> readSkills(JsonNode skills) {
        if (skills == null || !skills.isArray()) {
            return List.of();
        }
        List<ParsedSkillDTO> values = new ArrayList<>();
        skills.forEach(item -> {
            if (item.isTextual()) {
                String name = item.asText().trim();
                if (!name.isEmpty()) {
                    values.add(ParsedSkillDTO.builder().name(name).build());
                }
            } else if (item.isObject()) {
                String name = firstText(item, "name", "skill");
                if (name != null) {
                    values.add(ParsedSkillDTO.builder().name(name).build());
                }
            }
        });
        return values;
    }

    private static List<ParsedExperienceDTO> readExperience(JsonNode experience) {
        if (experience == null || !experience.isArray()) {
            return List.of();
        }
        List<ParsedExperienceDTO> values = new ArrayList<>();
        for (JsonNode item : experience) {
            if (!item.isObject()) {
                continue;
            }
            String company = text(item, "company");
            if (company == null) {
                continue;
            }
            boolean current = item.has("current") && item.get("current").asBoolean(false);
            values.add(ParsedExperienceDTO.builder()
                    .company(company)
                    .title(text(item, "title"))
                    .startDate(parseDate(text(item, "startDate")))
                    .endDate(current ? null : parseDate(text(item, "endDate")))
                    .current(current)
                    .description(text(item, "description"))
                    .build());
        }
        return values;
    }

    private static List<ParsedEducationDTO> readEducation(JsonNode education) {
        if (education == null || !education.isArray()) {
            return List.of();
        }
        List<ParsedEducationDTO> values = new ArrayList<>();
        for (JsonNode item : education) {
            if (!item.isObject()) {
                continue;
            }
            String institution = text(item, "institution");
            if (institution == null) {
                continue;
            }
            LocalDate startDate = parseDate(text(item, "startDate"));
            LocalDate endDate = parseDate(text(item, "endDate"));
            if (startDate == null && item.hasNonNull("startYear")) {
                startDate = yearToDate(item.get("startYear").asInt());
            }
            if (endDate == null && item.hasNonNull("endYear")) {
                endDate = yearToDate(item.get("endYear").asInt());
            }
            values.add(ParsedEducationDTO.builder()
                    .institution(institution)
                    .degree(text(item, "degree"))
                    .fieldOfStudy(text(item, "fieldOfStudy"))
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());
        }
        return values;
    }

    private static List<ParsedLinkDTO> readLinks(JsonNode root) {
        List<ParsedLinkDTO> links = new ArrayList<>();
        addLink(links, LinkType.LINKEDIN, text(root, "linkedin"));
        addLink(links, LinkType.GITHUB, text(root, "github"));
        addLink(links, LinkType.PORTFOLIO, firstText(root, "portfolio", "website"));

        JsonNode linksNode = root.get("links");
        if (linksNode != null && linksNode.isArray()) {
            StreamSupport.stream(linksNode.spliterator(), false)
                    .filter(JsonNode::isObject)
                    .forEach(item -> {
                        String url = text(item, "url");
                        if (url == null) {
                            return;
                        }
                        LinkType type = parseLinkType(text(item, "type"));
                        addLink(links, type, url);
                    });
        }
        return links;
    }

    private static void addLink(List<ParsedLinkDTO> links, LinkType type, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        links.add(ParsedLinkDTO.builder().type(type).url(url.trim()).build());
    }

    private static LinkType parseLinkType(String raw) {
        if (raw == null) {
            return LinkType.OTHER;
        }
        return switch (raw.trim().toUpperCase()) {
            case "LINKEDIN" -> LinkType.LINKEDIN;
            case "GITHUB" -> LinkType.GITHUB;
            case "PORTFOLIO", "WEBSITE" -> LinkType.PORTFOLIO;
            default -> LinkType.OTHER;
        };
    }

    private static LocalDate parseDate(String raw) {
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

    private static LocalDate yearToDate(int year) {
        return LocalDate.of(year, 1, 1);
    }
}
