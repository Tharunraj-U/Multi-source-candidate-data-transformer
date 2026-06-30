package com.eightfold.candidate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProcessRequest {
    @NotNull
    private UUID candidateId;
}
