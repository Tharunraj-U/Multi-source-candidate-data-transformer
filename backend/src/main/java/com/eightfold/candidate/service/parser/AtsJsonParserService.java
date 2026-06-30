package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.exception.SourceParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            ParsedCandidateDTO.ParsedCandidateDTOBuilder builder = ParsedCandidateDTO.builder()
                    .sourceType(SourceType.ATS_JSON)
                    .sourceId(source.getSourceId());

            if (root.hasNonNull("fullName")) builder.fullName(root.get("fullName").asText());
            if (root.hasNonNull("name")) builder.fullName(root.get("name").asText());
            if (root.hasNonNull("headline")) builder.headline(root.get("headline").asText());
            if (root.hasNonNull("location")) builder.location(root.get("location").asText());
            if (root.hasNonNull("email")) builder.emails(List.of(root.get("email").asText()));
            if (root.has("emails") && root.get("emails").isArray()) {
                List<String> emails = new ArrayList<>();
                root.get("emails").forEach(n -> emails.add(n.asText()));
                builder.emails(emails);
            }
            return builder.build();
        } catch (Exception ex) {
            throw new SourceParseException("ATS_PARSE_ERROR", "Failed to parse ATS JSON: " + ex.getMessage(), ex);
        }
    }
}
