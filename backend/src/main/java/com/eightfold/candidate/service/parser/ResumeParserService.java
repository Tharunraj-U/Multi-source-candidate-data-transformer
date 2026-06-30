package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.exception.SourceParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class ResumeParserService implements SourceParser {

    private final Tika tika = new Tika();

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.RESUME;
    }

    @Override
    public ParsedCandidateDTO parse(RawSource source) {
        if (source.getStoragePath() == null || source.getStoragePath().isBlank()) {
            throw new SourceParseException("RESUME_MISSING_FILE", "Resume storage path is required");
        }

        Path filePath = Path.of(source.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new SourceParseException("RESUME_NOT_FOUND", "Resume file not found: " + source.getStoragePath());
        }

        try (InputStream input = Files.newInputStream(filePath)) {
            String text = tika.parseToString(input);
            if (text == null || text.isBlank()) {
                throw new SourceParseException("RESUME_EMPTY", "No text could be extracted from resume");
            }

            ParsedCandidateDTO result = ResumeTextExtractor.extract(text, source.getSourceId());
            result.setSourceType(SourceType.RESUME);
            log.debug("Parsed resume for source {}: name={}", source.getSourceId(), result.getFullName());
            return result;
        } catch (SourceParseException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new SourceParseException("RESUME_IO_ERROR", "Failed to read resume file", ex);
        } catch (Exception ex) {
            throw new SourceParseException("RESUME_CORRUPT", "Failed to parse resume: " + ex.getMessage(), ex);
        }
    }
}
