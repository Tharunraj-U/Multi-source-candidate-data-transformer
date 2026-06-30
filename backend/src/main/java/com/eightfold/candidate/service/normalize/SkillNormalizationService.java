package com.eightfold.candidate.service.normalize;

import com.eightfold.candidate.repository.SkillAliasRepository;
import org.springframework.stereotype.Service;

@Service
public class SkillNormalizationService {

    private final SkillAliasRepository skillAliasRepository;

    public SkillNormalizationService(SkillAliasRepository skillAliasRepository) {
        this.skillAliasRepository = skillAliasRepository;
    }

    public String canonicalize(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return skillName;
        }
        String trimmed = skillName.trim();
        return skillAliasRepository.findByAliasIgnoreCase(trimmed)
                .map(alias -> alias.getCanonicalName())
                .orElse(trimmed);
    }
}
