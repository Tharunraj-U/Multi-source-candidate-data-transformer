package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.exception.SourceParseException;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecruiterCsvParserService implements SourceParser {

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.RECRUITER_CSV;
    }

    @Override
    public ParsedCandidateDTO parse(RawSource source) {
        if (source.getStoragePath() == null) {
            throw new SourceParseException("CSV_MISSING_FILE", "Recruiter CSV storage path is required");
        }
        try {
            List<String> lines = Files.readAllLines(Path.of(source.getStoragePath()));
            if (lines.size() < 2) {
                throw new SourceParseException("CSV_EMPTY", "CSV must have a header and at least one data row");
            }
            String[] headers = lines.getFirst().split(",");
            String[] values = lines.get(1).split(",", -1);

            ParsedCandidateDTO.ParsedCandidateDTOBuilder builder = ParsedCandidateDTO.builder()
                    .sourceType(SourceType.RECRUITER_CSV)
                    .sourceId(source.getSourceId());

            List<String> emails = new ArrayList<>();
            for (int i = 0; i < headers.length && i < values.length; i++) {
                String header = headers[i].trim().toLowerCase();
                String value = values[i].trim();
                if (value.isEmpty()) continue;
                switch (header) {
                    case "name", "full_name", "fullname" -> builder.fullName(value);
                    case "email" -> emails.add(value);
                    case "headline", "title" -> builder.headline(value);
                    case "location" -> builder.location(value);
                    case "company" -> { /* experience merge handles separately in future */ }
                    default -> { }
                }
            }
            if (!emails.isEmpty()) builder.emails(emails);
            return builder.build();
        } catch (SourceParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SourceParseException("CSV_PARSE_ERROR", "Failed to parse recruiter CSV: " + ex.getMessage(), ex);
        }
    }
}
