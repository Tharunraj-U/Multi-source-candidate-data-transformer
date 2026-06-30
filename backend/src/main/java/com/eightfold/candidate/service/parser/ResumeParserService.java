package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.exception.SourceParseException;
import com.eightfold.candidate.service.extraction.ResumeLlmExtractionService;
import com.eightfold.candidate.service.extraction.ResumeTextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Resume pipeline: file → Tika/Docling text → Ollama prompt/JSON → ParsedCandidateDTO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParserService implements SourceParser {

    private final ResumeTextExtractionService textExtraction;
    private final ResumeLlmExtractionService llmExtraction;

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
        String text = textExtraction.extractFromPath(filePath);

        ParsedCandidateDTO heuristic = ResumeTextExtractor.extract(text, source.getSourceId());
        heuristic.setSourceType(SourceType.RESUME);

        if (llmExtraction.isEnabled()) {
            ParsedCandidateDTO result = llmExtraction.extractFromResumeText(
                    text, source.getSourceId(), heuristic);
            log.info("Resume parsed via LLM for source {}: name={}, experience={}, education={}",
                    source.getSourceId(), result.getFullName(),
                    result.getExperience().size(), result.getEducation().size());
            return result;
        }

        log.info("Resume parsed via heuristic for source {}: name={}", source.getSourceId(), heuristic.getFullName());
        return heuristic;
    }
}
