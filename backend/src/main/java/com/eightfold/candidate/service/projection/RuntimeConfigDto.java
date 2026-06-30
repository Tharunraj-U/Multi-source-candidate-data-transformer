package com.eightfold.candidate.service.projection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeConfigDto {

    private List<FieldMapping> fields;
    private boolean includeConfidence;
    private boolean includeProvenance;
    private String missing = "omit";

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldMapping {
        private String path;
        private String from;
    }
}
