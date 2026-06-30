package com.eightfold.candidate.service;

import com.eightfold.candidate.domain.entity.*;
import com.eightfold.candidate.domain.enums.CandidateStatus;
import com.eightfold.candidate.domain.enums.JobStatus;
import com.eightfold.candidate.domain.enums.SourceStatus;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.dto.response.ApiDtos;
import com.eightfold.candidate.exception.CandidateNotFoundException;
import com.eightfold.candidate.mapper.CandidateMapper;
import com.eightfold.candidate.repository.CandidateRepository;
import com.eightfold.candidate.repository.ProcessingJobRepository;
import com.eightfold.candidate.repository.RawSourceRepository;
import com.eightfold.candidate.repository.RuntimeConfigRepository;
import com.eightfold.candidate.service.storage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CandidateRepository candidateRepository;
    private final RawSourceRepository rawSourceRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final RuntimeConfigRepository runtimeConfigRepository;
    private final LocalFileStorageService fileStorage;
    private final CandidateMapper mapper;
    private final CandidateProcessingService processingService;

    @Transactional
    public ApiDtos.UploadResponseDto upload(
            MultipartFile resume,
            MultipartFile recruiterCsv,
            MultipartFile atsJson,
            String linkedInUrl,
            String gitHubUrl,
            String runtimeConfigJson) throws IOException {

        if (!hasAnySource(resume, recruiterCsv, atsJson, linkedInUrl, gitHubUrl)) {
            throw new IllegalArgumentException("At least one source is required");
        }

        Candidate candidate = Candidate.builder()
                .status(CandidateStatus.DRAFT)
                .build();
        candidate = candidateRepository.save(candidate);

        List<RawSource> sources = new ArrayList<>();

        if (resume != null && !resume.isEmpty()) {
            String path = fileStorage.store(
                    candidate.getCandidateId(), "resume",
                    resume.getOriginalFilename() != null ? resume.getOriginalFilename() : "resume.pdf",
                    resume.getInputStream());
            candidate.setResumePath(path);
            sources.add(createSource(candidate, SourceType.RESUME, path, resume.getOriginalFilename(), null));
        }
        if (recruiterCsv != null && !recruiterCsv.isEmpty()) {
            String path = fileStorage.store(
                    candidate.getCandidateId(), "csv",
                    recruiterCsv.getOriginalFilename() != null ? recruiterCsv.getOriginalFilename() : "recruiter.csv",
                    recruiterCsv.getInputStream());
            sources.add(createSource(candidate, SourceType.RECRUITER_CSV, path, recruiterCsv.getOriginalFilename(), null));
        }
        if (atsJson != null && !atsJson.isEmpty()) {
            String path = fileStorage.store(
                    candidate.getCandidateId(), "ats",
                    atsJson.getOriginalFilename() != null ? atsJson.getOriginalFilename() : "ats.json",
                    atsJson.getInputStream());
            sources.add(createSource(candidate, SourceType.ATS_JSON, path, atsJson.getOriginalFilename(), null));
        }
        if (linkedInUrl != null && !linkedInUrl.isBlank()) {
            sources.add(createSource(candidate, SourceType.LINKEDIN, null, null, linkedInUrl.trim()));
        }
        if (gitHubUrl != null && !gitHubUrl.isBlank()) {
            sources.add(createSource(candidate, SourceType.GITHUB, null, null, gitHubUrl.trim()));
        }

        rawSourceRepository.saveAll(sources);
        candidate.getRawSources().addAll(sources);

        if (runtimeConfigJson != null && !runtimeConfigJson.isBlank()) {
            runtimeConfigRepository.save(RuntimeConfig.builder()
                    .candidate(candidate)
                    .configJson(runtimeConfigJson.trim())
                    .build());
        }

        candidateRepository.save(candidate);

        return ApiDtos.UploadResponseDto.builder()
                .candidateId(candidate.getCandidateId())
                .status(candidate.getStatus())
                .sources(sources.stream().map(mapper::toSourceSummary).toList())
                .createdAt(candidate.getCreatedAt())
                .build();
    }

    @Transactional
    public ApiDtos.ProcessResponseDto enqueueProcessing(UUID candidateId) {
        ProcessingJob job = processingService.enqueue(candidateId);
        return ApiDtos.ProcessResponseDto.builder()
                .jobId(job.getJobId())
                .candidateId(candidateId)
                .status(job.getStatus())
                .build();
    }

    public void triggerProcessing(UUID candidateId, UUID jobId) {
        processingService.runAsync(candidateId, jobId);
    }

    public ApiDtos.ProcessResponseDto startProcessing(UUID candidateId) {
        ApiDtos.ProcessResponseDto response = enqueueProcessing(candidateId);
        triggerProcessing(candidateId, response.getJobId());
        return response;
    }

    @Transactional
    public void prepareReprocess(UUID candidateId) {
        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));

        List<RawSource> sources = rawSourceRepository.findByCandidateCandidateIdOrderByIngestedAtAsc(candidateId);
        for (RawSource source : sources) {
            source.setStatus(SourceStatus.PENDING);
            source.setErrorCode(null);
            source.setErrorMessage(null);
            source.setProcessedAt(null);
        }
        rawSourceRepository.saveAll(sources);
        candidate.setStatus(CandidateStatus.DRAFT);
        candidateRepository.save(candidate);
    }

    @Transactional
    public ApiDtos.ProcessResponseDto reprocess(UUID candidateId) {
        prepareReprocess(candidateId);
        return enqueueProcessing(candidateId);
    }

    @Transactional(readOnly = true)
    public ApiDtos.JobStatusResponseDto getJobStatus(UUID candidateId, UUID jobId) {
        ProcessingJob job = processingJobRepository.findByJobIdAndCandidateCandidateId(jobId, candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        return ApiDtos.JobStatusResponseDto.builder()
                .jobId(job.getJobId())
                .candidateId(candidateId)
                .status(job.getStatus())
                .errorMessage(job.getErrorMessage())
                .build();
    }

    @Transactional(readOnly = true)
    public ApiDtos.CandidateResponseDto getCandidate(
            UUID candidateId, boolean includeProvenance, boolean includeConfidence) {
        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        return mapper.toResponse(candidate, includeProvenance, includeConfidence);
    }

    @Transactional(readOnly = true)
    public ApiDtos.CandidateListResponseDto listCandidates(
            String search,
            BigDecimal minConfidence,
            String company,
            CandidateStatus status,
            int page,
            int size,
            String sort) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Specification<Candidate> spec = Specification.where(CandidateSpecifications.notDeleted());
        if (search != null && !search.isBlank()) {
            spec = spec.and(CandidateSpecifications.search(search.trim()));
        }
        if (minConfidence != null) {
            spec = spec.and(CandidateSpecifications.minConfidence(minConfidence));
        }
        if (company != null && !company.isBlank()) {
            spec = spec.and(CandidateSpecifications.currentCompany(company.trim()));
        }
        if (status != null) {
            spec = spec.and(CandidateSpecifications.hasStatus(status));
        }

        Pageable pageable = PageRequest.of(safePage, safeSize, parseSort(sort));
        Page<Candidate> result = candidateRepository.findAll(spec, pageable);

        return ApiDtos.CandidateListResponseDto.builder()
                .content(result.getContent().stream().map(mapper::toListItem).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public void deleteCandidate(UUID candidateId) {
        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        candidate.setDeleted(true);
        candidateRepository.save(candidate);
    }

    @Transactional(readOnly = true)
    public InputStream getResumeStream(UUID candidateId) throws IOException {
        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        if (candidate.getResumePath() == null) {
            throw new CandidateNotFoundException(candidateId);
        }
        return fileStorage.retrieve(candidate.getResumePath());
    }

    @Transactional(readOnly = true)
    public InputStream getPictureStream(UUID candidateId) throws IOException {
        Candidate candidate = candidateRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        if (candidate.getProfilePicturePath() == null) {
            throw new CandidateNotFoundException(candidateId);
        }
        return fileStorage.retrieve(candidate.getProfilePicturePath());
    }

    private RawSource createSource(
            Candidate candidate, SourceType type, String storagePath,
            String filename, String url) {
        return RawSource.builder()
                .candidate(candidate)
                .sourceType(type)
                .storagePath(storagePath)
                .originalFilename(filename)
                .sourceUrl(url)
                .status(SourceStatus.PENDING)
                .build();
    }

    private boolean hasAnySource(
            MultipartFile resume, MultipartFile recruiterCsv, MultipartFile atsJson,
            String linkedInUrl, String gitHubUrl) {
        return (resume != null && !resume.isEmpty())
                || (recruiterCsv != null && !recruiterCsv.isEmpty())
                || (atsJson != null && !atsJson.isEmpty())
                || (linkedInUrl != null && !linkedInUrl.isBlank())
                || (gitHubUrl != null && !gitHubUrl.isBlank());
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
