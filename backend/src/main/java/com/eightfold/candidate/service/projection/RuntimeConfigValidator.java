package com.eightfold.candidate.service.projection;

import com.eightfold.candidate.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class RuntimeConfigValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RuntimeConfigDto parseAndValidate(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            throw new ValidationException("runtimeConfig must not be blank");
        }
        RuntimeConfigDto config;
        try {
            config = objectMapper.readValue(configJson, RuntimeConfigDto.class);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Invalid runtimeConfig JSON: " + ex.getOriginalMessage());
        }
        if (config.getFields() == null || config.getFields().isEmpty()) {
            throw new ValidationException("runtimeConfig.fields must contain at least one mapping");
        }
        for (RuntimeConfigDto.FieldMapping field : config.getFields()) {
            if (field.getPath() == null || field.getPath().isBlank()) {
                throw new ValidationException("Each runtimeConfig field requires a non-blank path");
            }
            if (field.getFrom() == null || field.getFrom().isBlank()) {
                throw new ValidationException("Each runtimeConfig field requires a non-blank from path");
            }
            CanonicalPathResolver.validatePath(field.getFrom());
        }
        String missing = config.getMissing();
        if (missing != null && !missing.isBlank()
                && !"omit".equalsIgnoreCase(missing) && !"null".equalsIgnoreCase(missing)) {
            throw new ValidationException("runtimeConfig.missing must be 'omit' or 'null'");
        }
        return config;
    }
}
