package com.eightfold.candidate.service;

import com.eightfold.candidate.domain.entity.*;
import com.eightfold.candidate.domain.enums.*;
import com.eightfold.candidate.domain.model.ParsedCandidateDTO;
import com.eightfold.candidate.exception.ProcessingConflictException;
import com.eightfold.candidate.exception.SourceParseException;
import com.eightfold.candidate.repository.CandidateRepository;
import com.eightfold.candidate.repository.ProcessingJobRepository;
import com.eightfold.candidate.repository.RawSourceRepository;
import com.eightfold.candidate.service.merge.CandidateProfileMerger;
import com.eightfold.candidate.service.parser.SourceParser;
import com.eightfold.candidate.service.parser.SourceParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateProcessingService {

    private final CandidateRepository candidateRepository;
    private final RawSourceRepository rawSourceRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final SourceParserFactory parserFactory;
    private final CandidateProfileMerger merger;

    @Transactional
    public ProcessingJob enqueue(UUID candidateId) {
        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new com.eightfold.candidate.exception.CandidateNotFoundException(candidateId));

        if (candidate.getStatus() == CandidateStatus.PROCESSING) {
            throw new ProcessingConflictException("Candidate is already processing");
        }

        candidate.setStatus(CandidateStatus.PROCESSING);
        ProcessingJob job = ProcessingJob.builder()
                .candidate(candidate)
                .status(JobStatus.QUEUED)
                .build();
        job = processingJobRepository.save(job);
        candidateRepository.save(candidate);
        return job;
    }

    @Async("processingExecutor")
    @Transactional
    public void runAsync(UUID candidateId, UUID jobId) {
        ProcessingJob job = processingJobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        processingJobRepository.save(job);

        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow();

        List<RawSource> sources = rawSourceRepository.findByCandidateCandidateIdOrderByIngestedAtAsc(candidateId);
        List<ParsedCandidateDTO> parsedList = new ArrayList<>();
        int failures = 0;

        for (RawSource source : sources) {
            source.setStatus(SourceStatus.PROCESSING);
            rawSourceRepository.save(source);
            try {
                SourceParser parser = parserFactory.getParser(source.getSourceType());
                ParsedCandidateDTO parsed = parser.parse(source);
                parsedList.add(parsed);
                source.setStatus(SourceStatus.COMPLETED);
                source.setProcessedAt(Instant.now());
            } catch (SourceParseException ex) {
                failures++;
                source.setStatus(SourceStatus.FAILED);
                source.setErrorCode(ex.getErrorCode());
                source.setErrorMessage(ex.getMessage());
                source.setProcessedAt(Instant.now());
                log.warn("Parse failed for source {}: {}", source.getSourceId(), ex.getMessage());
            }
            rawSourceRepository.save(source);
        }

        clearChildCollections(candidate);
        if (!parsedList.isEmpty()) {
            merger.merge(candidate, parsedList);
        }

        if (parsedList.isEmpty()) {
            candidate.setStatus(CandidateStatus.FAILED);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("All sources failed to parse");
        } else if (failures > 0) {
            candidate.setStatus(CandidateStatus.PARTIAL);
            job.setStatus(JobStatus.COMPLETED);
        } else {
            candidate.setStatus(CandidateStatus.COMPLETED);
            job.setStatus(JobStatus.COMPLETED);
        }

        job.setCompletedAt(Instant.now());
        candidateRepository.save(candidate);
        processingJobRepository.save(job);
    }

    private void clearChildCollections(Candidate candidate) {
        candidate.getEmails().clear();
        candidate.getPhones().clear();
        candidate.getSkills().clear();
        candidate.getExperience().clear();
        candidate.getEducation().clear();
        candidate.getLinks().clear();
        candidate.getConfidenceScores().clear();
        candidate.getProvenance().clear();
    }
}
