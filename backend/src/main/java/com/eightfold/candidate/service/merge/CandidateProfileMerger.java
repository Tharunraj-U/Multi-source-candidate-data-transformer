package com.eightfold.candidate.service.merge;

import com.eightfold.candidate.domain.entity.*;
import com.eightfold.candidate.domain.enums.LinkType;
import com.eightfold.candidate.domain.enums.SourceType;
import com.eightfold.candidate.domain.model.*;
import com.eightfold.candidate.service.normalize.EmailValidationService;
import com.eightfold.candidate.service.parser.ResumeTextExtractor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class CandidateProfileMerger {

    private static final List<SourceType> PRIORITY = List.of(
            SourceType.RESUME, SourceType.GITHUB, SourceType.ATS_JSON, SourceType.RECRUITER_CSV);

    private final EmailValidationService emailValidation;

    public CandidateProfileMerger(EmailValidationService emailValidation) {
        this.emailValidation = emailValidation;
    }

    public void merge(Candidate candidate, List<ParsedCandidateDTO> parsedList) {
        parsedList.sort(Comparator.comparingInt(p -> priorityIndex(p.getSourceType())));

        for (ParsedCandidateDTO parsed : parsedList) {
            mergeScalars(candidate, parsed);
            mergeEmails(candidate, parsed);
            mergePhones(candidate, parsed);
            mergeSkills(candidate, parsed);
            mergeExperience(candidate, parsed);
            mergeEducation(candidate, parsed);
            mergeLinks(candidate, parsed);
        }

        candidate.setOverallConfidence(computeOverall(parsedList));
        populateFieldConfidence(candidate);
    }

    private int priorityIndex(SourceType type) {
        int idx = PRIORITY.indexOf(type);
        return idx < 0 ? PRIORITY.size() : idx;
    }

    private void mergeScalars(Candidate candidate, ParsedCandidateDTO parsed) {
        if (candidate.getFullName() == null && parsed.getFullName() != null) {
            candidate.setFullName(parsed.getFullName());
        }
        if (candidate.getHeadline() == null && parsed.getHeadline() != null) {
            candidate.setHeadline(parsed.getHeadline());
        }
        if (candidate.getLocation() == null && parsed.getLocation() != null) {
            candidate.setLocation(parsed.getLocation());
        }
        if (candidate.getYearsExperience() == null && parsed.getYearsExperience() != null) {
            candidate.setYearsExperience(parsed.getYearsExperience());
        }
    }

    private void mergeEmails(Candidate candidate, ParsedCandidateDTO parsed) {
        if (parsed.getEmails() == null) return;
        Set<String> existing = new HashSet<>();
        candidate.getEmails().forEach(e -> existing.add(e.getEmailAddress().toLowerCase()));
        for (String email : parsed.getEmails()) {
            if (email != null && !existing.contains(email.toLowerCase())) {
                candidate.getEmails().add(CandidateEmail.builder()
                        .candidate(candidate)
                        .emailAddress(email)
                        .primary(candidate.getEmails().isEmpty())
                        .confidence(baseConfidence(parsed.getSourceType()))
                        .validationStatus(emailValidation.validate(email))
                        .build());
                existing.add(email.toLowerCase());
            }
        }
    }

    private void mergePhones(Candidate candidate, ParsedCandidateDTO parsed) {
        if (parsed.getPhones() == null) return;
        Set<String> existing = new HashSet<>();
        candidate.getPhones().forEach(p -> existing.add(p.getPhoneE164()));
        for (String phone : parsed.getPhones()) {
            if (phone != null && !existing.contains(phone)) {
                candidate.getPhones().add(CandidatePhone.builder()
                        .candidate(candidate)
                        .phoneE164(phone)
                        .primary(candidate.getPhones().isEmpty())
                        .confidence(baseConfidence(parsed.getSourceType()))
                        .build());
                existing.add(phone);
            }
        }
    }

    private void mergeSkills(Candidate candidate, ParsedCandidateDTO parsed) {
        if (parsed.getSkills() == null) return;
        Set<String> existing = new HashSet<>();
        candidate.getSkills().forEach(s -> existing.add(
                (s.getCanonicalSkill() != null ? s.getCanonicalSkill() : s.getSkillName()).toLowerCase()));
        for (ParsedSkillDTO skill : parsed.getSkills()) {
            String canonical = skill.getName();
            if (canonical != null && !existing.contains(canonical.toLowerCase())) {
                candidate.getSkills().add(CandidateSkill.builder()
                        .candidate(candidate)
                        .skillName(skill.getName())
                        .canonicalSkill(canonical)
                        .sourceType(parsed.getSourceType())
                        .confidence(baseConfidence(parsed.getSourceType()))
                        .build());
                existing.add(canonical.toLowerCase());
            }
        }
    }

    private void mergeExperience(Candidate candidate, ParsedCandidateDTO parsed) {
        if (parsed.getExperience() == null) return;
        for (ParsedExperienceDTO exp : ResumeTextExtractor.filterExperience(parsed.getExperience())) {
            candidate.getExperience().add(CandidateExperience.builder()
                    .candidate(candidate)
                    .company(exp.getCompany() != null ? exp.getCompany() : "Unknown")
                    .title(exp.getTitle())
                    .startDate(exp.getStartDate())
                    .endDate(exp.getEndDate())
                    .current(exp.isCurrent())
                    .description(exp.getDescription())
                    .sourceType(parsed.getSourceType())
                    .confidence(baseConfidence(parsed.getSourceType()))
                    .build());
        }
    }

    private void mergeEducation(Candidate candidate, ParsedCandidateDTO parsed) {
        if (parsed.getEducation() == null) return;
        for (ParsedEducationDTO edu : ResumeTextExtractor.filterEducation(parsed.getEducation())) {
            candidate.getEducation().add(CandidateEducation.builder()
                    .candidate(candidate)
                    .institution(edu.getInstitution() != null ? edu.getInstitution() : "Unknown")
                    .degree(edu.getDegree())
                    .fieldOfStudy(edu.getFieldOfStudy())
                    .startDate(edu.getStartDate())
                    .endDate(edu.getEndDate())
                    .sourceType(parsed.getSourceType())
                    .confidence(baseConfidence(parsed.getSourceType()))
                    .build());
        }
    }

    private void mergeLinks(Candidate candidate, ParsedCandidateDTO parsed) {
        if (parsed.getLinks() == null) return;
        Set<String> existing = new HashSet<>();
        candidate.getLinks().forEach(l -> existing.add(l.getUrl()));
        for (ParsedLinkDTO link : parsed.getLinks()) {
            if (link.getUrl() != null && !existing.contains(link.getUrl())) {
                candidate.getLinks().add(CandidateLink.builder()
                        .candidate(candidate)
                        .linkType(link.getType() != null ? link.getType() : LinkType.OTHER)
                        .url(link.getUrl())
                        .confidence(baseConfidence(parsed.getSourceType()))
                        .build());
                existing.add(link.getUrl());
            }
        }
    }

    private void populateFieldConfidence(Candidate candidate) {
        candidate.getConfidenceScores().clear();
        addConfidence(candidate, "full_name", candidate.getFullName() != null && !candidate.getFullName().isBlank()
                ? score("0.95") : score("0.30"));
        addConfidence(candidate, "emails", candidate.getEmails().isEmpty() ? score("0.30") : score("0.95"));
        addConfidence(candidate, "phones", candidate.getPhones().isEmpty() ? score("0.30") : score("0.95"));
        addConfidence(candidate, "experience", candidate.getExperience().isEmpty() ? score("0.30") : score("0.90"));
        addConfidence(candidate, "education", candidate.getEducation().isEmpty() ? score("0.30") : score("0.90"));
        addConfidence(candidate, "skills", candidate.getSkills().isEmpty() ? score("0.30") : score("0.85"));
    }

    private void addConfidence(Candidate candidate, String fieldPath, BigDecimal score) {
        candidate.getConfidenceScores().add(CandidateConfidence.builder()
                .candidate(candidate)
                .fieldPath(fieldPath)
                .score(score)
                .build());
    }

    private BigDecimal score(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal baseConfidence(SourceType type) {
        return switch (type) {
            case RESUME -> new BigDecimal("0.95");
            case LINKEDIN -> new BigDecimal("0.85");
            case GITHUB -> new BigDecimal("0.70");
            case ATS_JSON -> new BigDecimal("0.75");
            case RECRUITER_CSV -> new BigDecimal("0.50");
        };
    }

    private BigDecimal computeOverall(List<ParsedCandidateDTO> parsedList) {
        if (parsedList.isEmpty()) return null;
        double avg = parsedList.stream()
                .mapToDouble(p -> baseConfidence(p.getSourceType()).doubleValue())
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(4, RoundingMode.HALF_UP);
    }
}
