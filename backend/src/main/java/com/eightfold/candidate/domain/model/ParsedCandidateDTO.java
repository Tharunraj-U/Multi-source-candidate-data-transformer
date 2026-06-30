package com.eightfold.candidate.domain.model;

import com.eightfold.candidate.domain.enums.SourceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ParsedCandidateDTO {

    private SourceType sourceType;
    private UUID sourceId;

    private String fullName;
    private String headline;
    private String about;
    private String location;
    private BigDecimal yearsExperience;
    private String profilePictureUrl;

    @Builder.Default
    private List<String> emails = new ArrayList<>();

    @Builder.Default
    private List<String> phones = new ArrayList<>();

    @Builder.Default
    private List<ParsedSkillDTO> skills = new ArrayList<>();

    @Builder.Default
    private List<ParsedExperienceDTO> experience = new ArrayList<>();

    @Builder.Default
    private List<ParsedEducationDTO> education = new ArrayList<>();

    @Builder.Default
    private List<ParsedLinkDTO> links = new ArrayList<>();
}
