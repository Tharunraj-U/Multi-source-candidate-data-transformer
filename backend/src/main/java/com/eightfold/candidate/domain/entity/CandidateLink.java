package com.eightfold.candidate.domain.entity;

import com.eightfold.candidate.domain.enums.LinkType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "candidate_link",
        uniqueConstraints = @UniqueConstraint(name = "UQ_candidate_link", columnNames = {"candidate_id", "link_type", "url"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 30)
    private LinkType linkType;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;
}
