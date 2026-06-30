package com.eightfold.candidate.domain.entity;

import com.eightfold.candidate.domain.enums.CandidateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @UuidGenerator
    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(length = 500)
    private String headline;

    @Column(name = "years_experience", precision = 4, scale = 1)
    private BigDecimal yearsExperience;

    @Column(name = "overall_confidence", precision = 5, scale = 4)
    private BigDecimal overallConfidence;

    @Column(name = "profile_picture_path", length = 1000)
    private String profilePicturePath;

    @Column(name = "resume_path", length = 1000)
    private String resumePath;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CandidateStatus status = CandidateStatus.DRAFT;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RawSource> rawSources = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateEmail> emails = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidatePhone> phones = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateSkill> skills = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateExperience> experience = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateEducation> education = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateLink> links = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateProvenance> provenance = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CandidateConfidence> confidenceScores = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RuntimeConfig> runtimeConfigs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProcessingJob> processingJobs = new LinkedHashSet<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
