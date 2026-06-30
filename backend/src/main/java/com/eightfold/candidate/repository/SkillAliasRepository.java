package com.eightfold.candidate.repository;

import com.eightfold.candidate.domain.entity.SkillAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillAliasRepository extends JpaRepository<SkillAlias, Long> {

    Optional<SkillAlias> findByAliasIgnoreCase(String alias);
}
