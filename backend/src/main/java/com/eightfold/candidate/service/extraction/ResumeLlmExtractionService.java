package com.eightfold.candidate.service.extraction;

import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;

import java.util.UUID;

public interface ResumeLlmExtractionService {

    boolean isEnabled();

    ParsedCandidateDTO extractFromResumeText(
            String resumeText, UUID sourceId, ParsedCandidateDTO heuristicFallback);

    default ParsedCandidateDTO extractFromText(
            String text, SourceType sourceType, UUID sourceId, ParsedCandidateDTO fallback) {
        return extractFromResumeText(text, sourceId, fallback);
    }
}
