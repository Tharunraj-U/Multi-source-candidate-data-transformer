package com.eightfold.candidate.domain.entity;

import com.eightfold.candidate.domain.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "candidate_skill",
        uniqueConstraints = @UniqueConstraint(name = "UQ_candidate_skill", columnNames = {"candidate_id", "canonical_skill"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "skill_name", nullable = false, length = 255)
    private String skillName;

    @Column(name = "canonical_skill", length = 255)
    private String canonicalSkill;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private SourceType sourceType;
}
