package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.service.parser.ResumeContactSanitizer;
import com.eightfold.candidate.service.parser.ResumeTextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local LLM via Ollama Docker (OpenAI-compatible /v1/chat/completions).
 */
@Slf4j
@Service
public class OllamaProfileExtractionService implements ResumeLlmExtractionService {

    private static final Pattern JSON_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final RestClient llmClient;
    private final ObjectMapper objectMapper;
    private final ResumeProfileJsonMapper profileMapper;
    private final String model;
    private final boolean enabled;
    private final int maxInputChars;
    private final int maxOutputTokens;

    public OllamaProfileExtractionService(
            ObjectMapper objectMapper,
            ResumeProfileJsonMapper profileMapper,
            @Value("${ollama.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${ollama.model:qwen3}") String model,
            @Value("${ollama.enabled:true}") boolean enabled,
            @Value("${ollama.max-input-chars:6000}") int maxInputChars,
            @Value("${ollama.max-output-tokens:2048}") int maxOutputTokens) {
        this.objectMapper = objectMapper;
        this.profileMapper = profileMapper;
        this.model = model;
        this.enabled = enabled;
        this.maxInputChars = maxInputChars;
        this.maxOutputTokens = maxOutputTokens;
        this.llmClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ParsedCandidateDTO extractFromResumeText(
            String resumeText, UUID sourceId, ParsedCandidateDTO heuristicFallback) {
        if (!isEnabled()) {
            return heuristicFallback != null ? heuristicFallback : emptyDto(sourceId);
        }
        try {
            String prompt = ResumeExtractionPrompt.build(ResumeExtractionPrompt.truncate(resumeText, maxInputChars));
            ParsedCandidateDTO extracted = callOllama(prompt, sourceId);
            return merge(heuristicFallback, extracted);
        } catch (Exception ex) {
            log.warn("Ollama extraction failed: {}", ex.getMessage());
            return heuristicFallback != null ? heuristicFallback : emptyDto(sourceId);
        }
    }

    private ParsedCandidateDTO callOllama(String prompt, UUID sourceId) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new ChatRequest(model, prompt, maxOutputTokens));

        JsonNode response = llmClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        String raw = response.path("choices").path(0).path("message").path("content").asText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Ollama returned empty content");
        }
        String jsonText = unwrapJson(raw);
        return profileMapper.fromJsonString(jsonText, SourceType.RESUME, sourceId);
    }

    static String unwrapJson(String raw) {
        String trimmed = raw.trim();
        Matcher matcher = JSON_FENCE.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }

    private ParsedCandidateDTO merge(ParsedCandidateDTO base, ParsedCandidateDTO extracted) {
        if (base == null) return extracted;
        if (extracted == null) return base;
        return ParsedCandidateDTO.builder()
                .sourceType(SourceType.RESUME)
                .sourceId(pick(base.getSourceId(), extracted.getSourceId()))
                .fullName(pick(extracted.getFullName(), base.getFullName()))
                .headline(pick(extracted.getHeadline(), base.getHeadline()))
                .about(pick(extracted.getAbout(), base.getAbout()))
                .location(pick(extracted.getLocation(), base.getLocation()))
                .yearsExperience(pick(extracted.getYearsExperience(), base.getYearsExperience()))
                .emails(mergeEmails(extracted.getEmails(), base.getEmails()))
                .phones(mergeLists(extracted.getPhones(), base.getPhones()))
                .skills(mergeSkills(extracted.getSkills(), base.getSkills()))
                .experience(chooseExperience(extracted.getExperience(), base.getExperience()))
                .education(chooseEducation(extracted.getEducation(), base.getEducation()))
                .links(mergeLinks(extracted.getLinks(), base.getLinks(),
                        extracted.getEmails(), base.getEmails()))
                .build();
    }

    private List<ParsedExperienceDTO> chooseExperience(
            List<ParsedExperienceDTO> llm, List<ParsedExperienceDTO> heuristic) {
        List<ParsedExperienceDTO> a = ResumeTextExtractor.filterExperience(llm);
        List<ParsedExperienceDTO> b = ResumeTextExtractor.filterExperience(heuristic);
        return scoreExperience(a) >= scoreExperience(b) ? a : b;
    }

    private List<ParsedEducationDTO> chooseEducation(
            List<ParsedEducationDTO> llm, List<ParsedEducationDTO> heuristic) {
        List<ParsedEducationDTO> a = ResumeTextExtractor.filterEducation(llm);
        List<ParsedEducationDTO> b = ResumeTextExtractor.filterEducation(heuristic);
        return scoreEducation(a) >= scoreEducation(b) ? a : b;
    }

    private int scoreExperience(List<ParsedExperienceDTO> items) {
        if (items == null || items.isEmpty()) return 0;
        int score = items.size() * 5;
        for (ParsedExperienceDTO item : items) {
            if (item.getStartDate() != null) score += 3;
            if (item.getCompany() != null && item.getTitle() != null) score += 2;
        }
        return score;
    }

    private int scoreEducation(List<ParsedEducationDTO> items) {
        if (items == null || items.isEmpty()) return 0;
        int score = items.size() * 5;
        for (ParsedEducationDTO item : items) {
            if (item.getInstitution() != null) score += 2;
            if (item.getDegree() != null) score += 2;
            if (item.getStartDate() != null || item.getEndDate() != null) score += 3;
        }
        return score;
    }

    private <T> T pick(T preferred, T fallback) {
        if (preferred == null) return fallback;
        if (preferred instanceof String s && s.isBlank()) return fallback;
        return preferred;
    }

    private List<String> mergeEmails(List<String> primary, List<String> secondary) {
        return ResumeContactSanitizer.filterEmails(mergeLists(primary, secondary));
    }

    private List<ParsedLinkDTO> mergeLinks(
            List<ParsedLinkDTO> primary, List<ParsedLinkDTO> secondary,
            List<String> primaryEmails, List<String> secondaryEmails) {
        java.util.ArrayList<ParsedLinkDTO> merged = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        if (primary != null) {
            for (ParsedLinkDTO link : primary) {
                if (link.getUrl() != null && seen.add(link.getUrl().toLowerCase())) {
                    merged.add(link);
                }
            }
        }
        if (secondary != null) {
            for (ParsedLinkDTO link : secondary) {
                if (link.getUrl() != null && seen.add(link.getUrl().toLowerCase())) {
                    merged.add(link);
                }
            }
        }
        for (ParsedLinkDTO recovered : ResumeContactSanitizer.linksFromMisplacedContacts(
                mergeLists(primaryEmails, secondaryEmails), List.of())) {
            if (seen.add(recovered.getUrl().toLowerCase())) {
                merged.add(recovered);
            }
        }
        return merged;
    }

    private List<String> mergeLists(List<String> primary, List<String> secondary) {
        java.util.ArrayList<String> merged = new java.util.ArrayList<>();
        if (primary != null) merged.addAll(primary);
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
        java.util.ArrayList<ParsedSkillDTO> merged = new java.util.ArrayList<>();
        if (primary != null) merged.addAll(primary);
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

    private static ParsedCandidateDTO emptyDto(UUID sourceId) {
        return ParsedCandidateDTO.builder().sourceType(SourceType.RESUME).sourceId(sourceId).build();
    }

    private record ChatRequest(String model, List<Message> messages, double temperature,
                               int max_tokens, ResponseFormat response_format) {
        ChatRequest(String model, String prompt, int maxTokens) {
            this(model, List.of(new Message("user", prompt)), 0.1, maxTokens,
                    new ResponseFormat("json_object"));
        }
    }

    private record Message(String role, String content) {}

    private record ResponseFormat(String type) {}
}
