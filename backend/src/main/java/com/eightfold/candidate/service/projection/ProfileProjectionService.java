package com.eightfold.candidate.service.projection;

import com.eightfold.candidate.dto.response.ApiDtos;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProfileProjectionService {

    private final RuntimeConfigValidator configValidator;

    public ProfileProjectionService(RuntimeConfigValidator configValidator) {
        this.configValidator = configValidator;
    }

    public RuntimeConfigDto parseConfig(String configJson) {
        return configValidator.parseAndValidate(configJson);
    }

    public Map<String, Object> project(ApiDtos.CandidateResponseDto profile, String configJson) {
        RuntimeConfigDto config = parseConfig(configJson);
        return project(profile, config);
    }

    public Map<String, Object> project(ApiDtos.CandidateResponseDto profile, RuntimeConfigDto config) {
        boolean omitMissing = !"null".equalsIgnoreCase(config.getMissing());
        Map<String, Object> output = new LinkedHashMap<>();

        for (RuntimeConfigDto.FieldMapping mapping : config.getFields()) {
            Object value = CanonicalPathResolver.resolve(profile, mapping.getFrom());
            if (value == null) {
                if (omitMissing) {
                    continue;
                }
                output.put(mapping.getPath(), null);
                continue;
            }
            output.put(mapping.getPath(), normalizeValue(value));
        }

        if (config.isIncludeConfidence() && profile.getConfidence() != null) {
            output.put("confidence", CanonicalPathResolver.confidenceAsMap(profile.getConfidence()));
        }
        if (config.isIncludeProvenance() && profile.getProvenance() != null) {
            output.put("provenance", profile.getProvenance());
        }

        return output;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value;
    }
}
