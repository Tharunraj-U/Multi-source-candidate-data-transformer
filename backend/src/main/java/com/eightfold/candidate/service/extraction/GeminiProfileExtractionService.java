package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.service.parser.ResumeTextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class GeminiProfileExtractionService {

    private static final String PROMPT_PREFIX = """
            Extract resume profile as JSON. Rules:
            - experience[]: real jobs only — each entry needs company (employer name), title (job title), \
            startDate, endDate or current=true. Put bullet achievements in description, NOT in title/company.
            - education[]: each entry needs institution (school name) and degree (e.g. B.Tech, B.S.).
            - Do not use section headers (WORK EXPERIENCE, EDUCATION) as field values.
            - Dates as YYYY-MM or YYYY. null if missing.
            Also: fullName, headline, location, yearsExperience, emails[], phones[], skills[].

            """;

    private final RestClient geminiClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final int maxInputChars;
    private final int maxOutputTokens;

    public GeminiProfileExtractionService(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            @Value("${gemini.enabled:true}") boolean enabled,
            @Value("${gemini.max-input-chars:6000}") int maxInputChars,
            @Value("${gemini.max-output-tokens:1024}") int maxOutputTokens) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        this.maxInputChars = maxInputChars;
        this.maxOutputTokens = maxOutputTokens;
        this.geminiClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public ParsedCandidateDTO extractFromText(
            String text, SourceType sourceType, UUID sourceId, ParsedCandidateDTO fallback) {
        if (!isEnabled()) {
            return fallback != null ? fallback : emptyDto(sourceType, sourceId);
        }
        try {
            ParsedCandidateDTO extracted = callGemini(truncate(text), sourceType, sourceId);
            return merge(fallback, extracted);
        } catch (Exception ex) {
            log.warn("Gemini extraction failed for {}: {}", sourceType, ex.getMessage());
            return fallback != null ? fallback : emptyDto(sourceType, sourceId);
        }
    }

    private ParsedCandidateDTO callGemini(String text, SourceType sourceType, UUID sourceId) throws Exception {
        String prompt = PROMPT_PREFIX + "SOURCE:" + sourceType.name() + "\n\n" + text;

        String requestBody = objectMapper.writeValueAsString(new GeminiRequest(prompt, maxOutputTokens));

        JsonNode response = geminiClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        String jsonText = extractResponseText(response);
        JsonNode profile = objectMapper.readTree(jsonText);
        return mapProfile(profile, sourceType, sourceId);
    }

    private String extractResponseText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Empty Gemini response");
        }
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini returned no content");
        }
        return parts.get(0).path("text").asText();
    }

    private ParsedCandidateDTO mapProfile(JsonNode node, SourceType sourceType, UUID sourceId) {
        ParsedCandidateDTO.ParsedCandidateDTOBuilder builder = ParsedCandidateDTO.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .fullName(text(node, "fullName"))
                .headline(text(node, "headline"))
                .location(text(node, "location"))
                .yearsExperience(decimal(node, "yearsExperience"));

        builder.emails(stringList(node, "emails"));
        builder.phones(stringList(node, "phones"));
        builder.skills(mapSkills(node.path("skills")));
        builder.experience(ResumeTextExtractor.filterExperience(mapExperience(node.path("experience"))));
        builder.education(ResumeTextExtractor.filterEducation(mapEducation(node.path("education"))));
        return builder.build();
    }

    private List<ParsedSkillDTO> mapSkills(JsonNode skills) {
        List<ParsedSkillDTO> result = new ArrayList<>();
        if (!skills.isArray()) {
            return result;
        }
        for (JsonNode skill : skills) {
            String name = skill.isTextual() ? skill.asText() : text(skill, "name");
            if (name != null && !name.isBlank()) {
                result.add(ParsedSkillDTO.builder().name(name.trim()).build());
            }
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
            result.add(ParsedEducationDTO.builder()
                    .institution(text(item, "institution"))
                    .degree(text(item, "degree"))
                    .fieldOfStudy(text(item, "fieldOfStudy"))
                    .startDate(parseDate(text(item, "startDate")))
                    .endDate(parseDate(text(item, "endDate")))
                    .build());
        }
        return result;
    }

    private ParsedCandidateDTO merge(ParsedCandidateDTO base, ParsedCandidateDTO extracted) {
        if (base == null) {
            return extracted;
        }
        if (extracted == null) {
            return base;
        }
        return ParsedCandidateDTO.builder()
                .sourceType(pick(base.getSourceType(), extracted.getSourceType()))
                .sourceId(pick(base.getSourceId(), extracted.getSourceId()))
                .fullName(pick(extracted.getFullName(), base.getFullName()))
                .headline(pick(extracted.getHeadline(), base.getHeadline()))
                .about(pick(extracted.getAbout(), base.getAbout()))
                .location(pick(extracted.getLocation(), base.getLocation()))
                .yearsExperience(pick(extracted.getYearsExperience(), base.getYearsExperience()))
                .profilePictureUrl(pick(base.getProfilePictureUrl(), extracted.getProfilePictureUrl()))
                .emails(mergeLists(extracted.getEmails(), base.getEmails()))
                .phones(mergeLists(extracted.getPhones(), base.getPhones()))
                .skills(mergeSkills(extracted.getSkills(), base.getSkills()))
                .experience(chooseExperience(extracted.getExperience(), base.getExperience()))
                .education(chooseEducation(extracted.getEducation(), base.getEducation()))
                .links(!base.getLinks().isEmpty() ? base.getLinks() : extracted.getLinks())
                .build();
    }

    private List<ParsedExperienceDTO> chooseExperience(
            List<ParsedExperienceDTO> gemini, List<ParsedExperienceDTO> heuristic) {
        List<ParsedExperienceDTO> filteredGemini = ResumeTextExtractor.filterExperience(gemini);
        List<ParsedExperienceDTO> filteredHeuristic = ResumeTextExtractor.filterExperience(heuristic);
        return scoreExperience(filteredGemini) >= scoreExperience(filteredHeuristic)
                ? filteredGemini : filteredHeuristic;
    }

    private List<ParsedEducationDTO> chooseEducation(
            List<ParsedEducationDTO> gemini, List<ParsedEducationDTO> heuristic) {
        List<ParsedEducationDTO> filteredGemini = ResumeTextExtractor.filterEducation(gemini);
        List<ParsedEducationDTO> filteredHeuristic = ResumeTextExtractor.filterEducation(heuristic);
        return scoreEducation(filteredGemini) >= scoreEducation(filteredHeuristic)
                ? filteredGemini : filteredHeuristic;
    }

    private int scoreExperience(List<ParsedExperienceDTO> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int score = items.size() * 5;
        for (ParsedExperienceDTO item : items) {
            if (item.getStartDate() != null) {
                score += 3;
            }
            if (item.getCompany() != null && item.getTitle() != null) {
                score += 2;
            }
        }
        return score;
    }

    private int scoreEducation(List<ParsedEducationDTO> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int score = items.size() * 5;
        for (ParsedEducationDTO item : items) {
            if (item.getInstitution() != null) {
                score += 2;
            }
            if (item.getDegree() != null) {
                score += 2;
            }
            if (item.getStartDate() != null || item.getEndDate() != null) {
                score += 3;
            }
        }
        return score;
    }

    private <T> T pick(T preferred, T fallback) {
        if (preferred == null) {
            return fallback;
        }
        if (preferred instanceof String s && s.isBlank()) {
            return fallback;
        }
        return preferred;
    }

    private List<String> mergeLists(List<String> primary, List<String> secondary) {
        List<String> merged = new ArrayList<>();
        if (primary != null) {
            merged.addAll(primary);
        }
        if (secondary != null) {
            for (String item : secondary) {
                if (item != null && merged.stream().noneMatch(e -> e.equalsIgnoreCase(item))) {
                    merged.add(item);
                }
            }
        }
        return merged;
    }

    private List<ParsedSkillDTO> mergeSkills(List<ParsedSkillDTO> primary, List<ParsedSkillDTO> secondary) {
        List<ParsedSkillDTO> merged = new ArrayList<>();
        if (primary != null) {
            merged.addAll(primary);
        }
        if (secondary != null) {
            for (ParsedSkillDTO skill : secondary) {
                if (skill.getName() != null && merged.stream().noneMatch(
                        s -> skill.getName().equalsIgnoreCase(s.getName()))) {
                    merged.add(skill);
                }
            }
        }
        return merged;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r", "").trim();
        if (normalized.length() <= maxInputChars) {
            return normalized;
        }
        int head = (int) (maxInputChars * 0.65);
        int tail = maxInputChars - head - 20;
        return normalized.substring(0, head) + "\n...[truncated]...\n"
                + normalized.substring(normalized.length() - tail);
    }

    private static ParsedCandidateDTO emptyDto(SourceType sourceType, UUID sourceId) {
        return ParsedCandidateDTO.builder().sourceType(sourceType).sourceId(sourceId).build();
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

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if ("present".equalsIgnoreCase(value) || "current".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            if (value.length() == 4) {
                return LocalDate.of(Integer.parseInt(value), 1, 1);
            }
            if (value.length() == 7) {
                return YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NumberFormatException ex) {
            return null;
        }
    }

    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
        GeminiRequest(String prompt, int maxOutputTokens) {
            this(
                    List.of(new Content(List.of(new Part(prompt)))),
                    new GenerationConfig("application/json", 0.1, maxOutputTokens));
        }
    }

    private record Content(List<Part> parts) {}

    private record Part(String text) {}

    private record GenerationConfig(String responseMimeType, double temperature, int maxOutputTokens) {}
}
