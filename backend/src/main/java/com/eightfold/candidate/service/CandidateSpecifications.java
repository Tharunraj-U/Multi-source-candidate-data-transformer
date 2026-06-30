package com.eightfold.candidate.service;

import com.eightfold.candidate.domain.entity.Candidate;
import com.eightfold.candidate.domain.entity.CandidateExperience;
import com.eightfold.candidate.domain.enums.CandidateStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class CandidateSpecifications {

    private CandidateSpecifications() {}

    public static Specification<Candidate> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Candidate> hasStatus(CandidateStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Candidate> minConfidence(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("overallConfidence"), min);
    }

    public static Specification<Candidate> search(String term) {
        String pattern = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> emails = root.join("emails", JoinType.LEFT);
            Join<Object, Object> experience = root.join("experience", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(emails.get("emailAddress")), pattern),
                    cb.like(cb.lower(experience.get("company")), pattern)
            );
        };
    }

    public static Specification<Candidate> currentCompany(String company) {
        String pattern = "%" + company.toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Candidate, CandidateExperience> experience = root.join("experience", JoinType.LEFT);
            return cb.and(
                    cb.isTrue(experience.get("current")),
                    cb.like(cb.lower(experience.get("company")), pattern)
            );
        };
    }
}
