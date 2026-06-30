package com.eightfold.candidate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "candidate_email",
        uniqueConstraints = @UniqueConstraint(name = "UQ_candidate_email", columnNames = {"candidate_id", "email_address"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "email_address", nullable = false, length = 255)
    private String emailAddress;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "validation_status", length = 20)
    private String validationStatus;
}
