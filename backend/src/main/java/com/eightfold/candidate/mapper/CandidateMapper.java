package com.eightfold.candidate.mapper;

import com.eightfold.candidate.domain.entity.*;
import com.eightfold.candidate.dto.response.ApiDtos;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class CandidateMapper {

    public ApiDtos.CandidateResponseDto toResponse(Candidate c, boolean includeProvenance, boolean includeConfidence) {
        String id = c.getCandidateId().toString();
        return ApiDtos.CandidateResponseDto.builder()
                .candidateId(c.getCandidateId())
                .fullName(c.getFullName())
                .headline(c.getHeadline())
                .yearsExperience(c.getYearsExperience())
                .overallConfidence(c.getOverallConfidence())
                .profilePicturePath(c.getProfilePicturePath() != null
                        ? "/api/v1/candidate/" + id + "/picture" : null)
                .resumePath(c.getResumePath() != null
                        ? "/api/v1/candidate/" + id + "/resume" : null)
                .emails(c.getEmails().stream().map(e -> ApiDtos.EmailDto.builder()
                        .address(e.getEmailAddress())
                        .validationStatus(e.getValidationStatus())
                        .build()).toList())
                .phones(c.getPhones().stream().map(CandidatePhone::getPhoneE164).toList())
                .location(c.getLocation())
                .skills(c.getSkills().stream().map(s -> ApiDtos.SkillDto.builder()
                        .name(s.getSkillName())
                        .canonical(s.getCanonicalSkill())
                        .confidence(s.getConfidence())
                        .build()).toList())
                .experience(c.getExperience().stream().map(e -> ApiDtos.ExperienceDto.builder()
                        .company(e.getCompany())
                        .title(e.getTitle())
                        .startDate(e.getStartDate() != null ? e.getStartDate().toString() : null)
                        .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                        .isCurrent(e.isCurrent())
                        .description(e.getDescription())
                        .confidence(e.getConfidence())
                        .build()).toList())
                .education(c.getEducation().stream().map(e -> ApiDtos.EducationDto.builder()
                        .institution(e.getInstitution())
                        .degree(e.getDegree())
                        .fieldOfStudy(e.getFieldOfStudy())
                        .startDate(e.getStartDate() != null ? e.getStartDate().toString() : null)
                        .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                        .confidence(e.getConfidence())
                        .build()).toList())
                .links(c.getLinks().stream().map(l -> ApiDtos.LinkDto.builder()
                        .type(l.getLinkType().name())
                        .url(l.getUrl())
                        .confidence(l.getConfidence())
                        .build()).toList())
                .provenance(includeProvenance ? c.getProvenance().stream().map(p -> ApiDtos.ProvenanceDto.builder()
                        .fieldPath(p.getFieldPath())
                        .value(p.getSourceValue())
                        .sourceType(p.getSourceType())
                        .sourceId(p.getSource() != null ? p.getSource().getSourceId() : null)
                        .capturedAt(p.getCapturedAt())
                        .rawValue(p.getRawValue())
                        .build()).toList() : List.of())
                .confidence(includeConfidence ? c.getConfidenceScores().stream()
                        .collect(LinkedHashMap::new,
                                (m, v) -> m.put(v.getFieldPath(), v.getScore()),
                                LinkedHashMap::putAll) : null)
                .status(c.getStatus())
                .updatedAt(c.getUpdatedAt())
                .sources(c.getRawSources().stream().map(this::toRawSourceDto).toList())
                .build();
    }

    public ApiDtos.CandidateListItemDto toListItem(Candidate c) {
        CandidateEmail primary = c.getEmails().stream()
                .filter(CandidateEmail::isPrimary)
                .findFirst()
                .orElse(c.getEmails().stream().findFirst().orElse(null));

        String primaryEmail = primary != null ? primary.getEmailAddress() : null;
        String primaryEmailValidationStatus = primary != null ? primary.getValidationStatus() : null;

        String currentCompany = c.getExperience().stream()
                .filter(CandidateExperience::isCurrent)
                .map(CandidateExperience::getCompany)
                .findFirst()
                .orElse(c.getExperience().stream()
                        .max(Comparator.comparing(CandidateExperience::getStartDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(CandidateExperience::getCompany)
                        .orElse(null));

        String id = c.getCandidateId().toString();
        return ApiDtos.CandidateListItemDto.builder()
                .candidateId(c.getCandidateId())
                .fullName(c.getFullName())
                .primaryEmail(primaryEmail)
                .primaryEmailValidationStatus(primaryEmailValidationStatus)
                .currentCompany(currentCompany)
                .yearsExperience(c.getYearsExperience())
                .overallConfidence(c.getOverallConfidence())
                .profilePicturePath(c.getProfilePicturePath() != null
                        ? "/api/v1/candidate/" + id + "/picture" : null)
                .status(c.getStatus())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public ApiDtos.RawSourceDto toRawSourceDto(RawSource s) {
        return ApiDtos.RawSourceDto.builder()
                .sourceId(s.getSourceId())
                .sourceType(s.getSourceType())
                .status(s.getStatus())
                .originalFilename(s.getOriginalFilename())
                .sourceUrl(s.getSourceUrl())
                .errorCode(s.getErrorCode())
                .errorMessage(s.getErrorMessage())
                .ingestedAt(s.getIngestedAt())
                .build();
    }

    public ApiDtos.SourceSummaryDto toSourceSummary(RawSource s) {
        return ApiDtos.SourceSummaryDto.builder()
                .sourceId(s.getSourceId())
                .sourceType(s.getSourceType())
                .status(s.getStatus())
                .build();
    }
}
