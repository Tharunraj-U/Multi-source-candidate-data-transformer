package com.eightfold.candidate.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "skill_alias",
        uniqueConstraints = @UniqueConstraint(name = "UQ_skill_alias", columnNames = "alias"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String alias;

    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
