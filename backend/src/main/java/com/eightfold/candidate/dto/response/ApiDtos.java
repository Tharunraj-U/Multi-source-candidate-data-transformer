package com.eightfold.candidate.dto.response;

import com.eightfold.candidate.domain.enums.CandidateStatus;
import com.eightfold.candidate.domain.enums.JobStatus;
import com.eightfold.candidate.domain.enums.SourceStatus;
import com.eightfold.candidate.domain.enums.SourceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {

    private ApiDtos() {}

    @Data
    @Builder
    public static class SourceSummaryDto {
        private UUID sourceId;
        private SourceType sourceType;
        private SourceStatus status;
    }

    @Data
    @Builder
    public static class UploadResponseDto {
        private UUID candidateId;
        private CandidateStatus status;
        private List<SourceSummaryDto> sources;
        private Instant createdAt;
    }

    @Data
    @Builder
    public static class ProcessResponseDto {
        private UUID jobId;
        private UUID candidateId;
        private JobStatus status;
    }

    @Data
    @Builder
    public static class JobStatusResponseDto {
        private UUID jobId;
        private UUID candidateId;
        private JobStatus status;
        private String errorMessage;
    }

    @Data
    @Builder
    public static class SkillDto {
        private String name;
        private String canonical;
        private BigDecimal confidence;
    }

    @Data
    @Builder
    public static class ExperienceDto {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private Boolean isCurrent;
        private String description;
        private BigDecimal confidence;
    }

    @Data
    @Builder
    public static class EducationDto {
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private String startDate;
        private String endDate;
        private BigDecimal confidence;
    }

    @Data
    @Builder
    public static class LinkDto {
        private String type;
        private String url;
        private BigDecimal confidence;
    }

    @Data
    @Builder
    public static class ProvenanceDto {
        private String fieldPath;
        private String value;
        private SourceType sourceType;
        private UUID sourceId;
        private Instant capturedAt;
        private String rawValue;
    }

    @Data
    @Builder
    public static class RawSourceDto {
        private UUID sourceId;
        private SourceType sourceType;
        private SourceStatus status;
        private String originalFilename;
        private String sourceUrl;
        private String errorCode;
        private String errorMessage;
        private Instant ingestedAt;
    }

    @Data
    @Builder
    public static class CandidateResponseDto {
        private UUID candidateId;
        private String fullName;
        private String headline;
        private BigDecimal yearsExperience;
        private BigDecimal overallConfidence;
        private String profilePicturePath;
        private String resumePath;
        private List<String> emails;
        private List<String> phones;
        private String location;
        private List<SkillDto> skills;
        private List<ExperienceDto> experience;
        private List<EducationDto> education;
        private List<LinkDto> links;
        private List<ProvenanceDto> provenance;
        private Map<String, BigDecimal> confidence;
        private CandidateStatus status;
        private Instant updatedAt;
        private List<RawSourceDto> sources;
    }

    @Data
    @Builder
    public static class CandidateListItemDto {
        private UUID candidateId;
        private String fullName;
        private String primaryEmail;
        private String currentCompany;
        private BigDecimal yearsExperience;
        private BigDecimal overallConfidence;
        private String profilePicturePath;
        private CandidateStatus status;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class CandidateListResponseDto {
        private List<CandidateListItemDto> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
