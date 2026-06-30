package com.eightfold.candidate.repository;

import com.eightfold.candidate.domain.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID>, JpaSpecificationExecutor<Candidate> {

    @EntityGraph(attributePaths = {
            "emails", "phones", "skills", "experience", "education", "links",
            "provenance", "confidenceScores", "rawSources"
    })
    Optional<Candidate> findByCandidateIdAndDeletedFalse(UUID candidateId);

    @Override
    @EntityGraph(attributePaths = {"emails", "experience"})
    Page<Candidate> findAll(Specification<Candidate> spec, Pageable pageable);
}
