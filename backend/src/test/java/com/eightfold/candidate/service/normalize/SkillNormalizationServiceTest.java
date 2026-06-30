package com.eightfold.candidate.service.normalize;

import com.eightfold.candidate.domain.entity.SkillAlias;
import com.eightfold.candidate.repository.SkillAliasRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillNormalizationServiceTest {

    @Mock
    private SkillAliasRepository skillAliasRepository;

    @InjectMocks
    private SkillNormalizationService service;

    @Test
    void resolvesAliasToCanonical() {
        when(skillAliasRepository.findByAliasIgnoreCase("JS"))
                .thenReturn(Optional.of(SkillAlias.builder().alias("JS").canonicalName("JavaScript").build()));

        assertEquals("JavaScript", service.canonicalize("JS"));
    }

    @Test
    void keepsUnknownSkillAsIs() {
        when(skillAliasRepository.findByAliasIgnoreCase("Rust")).thenReturn(Optional.empty());

        assertEquals("Rust", service.canonicalize("Rust"));
    }
}
