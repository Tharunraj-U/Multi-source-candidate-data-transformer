package com.eightfold.candidate.domain.model;

import com.eightfold.candidate.domain.enums.LinkType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParsedLinkDTO {
    private LinkType type;
    private String url;
}
