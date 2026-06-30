package com.eightfold.candidate.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ParsedExperienceDTO {
    private String company;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;
    private String description;
}
