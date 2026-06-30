package com.eightfold.candidate.repository;

import com.eightfold.candidate.domain.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    Optional<ProcessingJob> findByJobIdAndCandidateCandidateId(UUID jobId, UUID candidateId);
}
