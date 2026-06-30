package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.entity.RawSource;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;

public interface SourceParser {

    boolean supports(SourceType sourceType);

    ParsedCandidateDTO parse(RawSource source);
}
