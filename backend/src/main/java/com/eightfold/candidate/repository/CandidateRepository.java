package com.eightfold.candidate.repository;

import com.eightfold.candidate.domain.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID>, JpaSpecificationExecutor<Candidate> {

    Optional<Candidate> findByCandidateIdAndDeletedFalse(UUID candidateId);
}
