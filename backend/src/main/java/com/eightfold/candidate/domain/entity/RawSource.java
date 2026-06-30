package com.eightfold.candidate.domain.entity;

import com.eightfold.candidate.domain.enums.SourceStatus;
import com.eightfold.candidate.domain.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "raw_source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawSource {

    @Id
    @UuidGenerator
    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SourceStatus status = SourceStatus.PENDING;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant ingestedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;
}
