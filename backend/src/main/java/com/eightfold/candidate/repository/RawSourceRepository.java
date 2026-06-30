package com.eightfold.candidate.repository;

import com.eightfold.candidate.domain.entity.RawSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RawSourceRepository extends JpaRepository<RawSource, UUID> {

    List<RawSource> findByCandidateCandidateIdOrderByIngestedAtAsc(UUID candidateId);
}
