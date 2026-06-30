package com.eightfold.candidate.repository;

import com.eightfold.candidate.domain.entity.RuntimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RuntimeConfigRepository extends JpaRepository<RuntimeConfig, Long> {

    Optional<RuntimeConfig> findFirstByCandidateCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
