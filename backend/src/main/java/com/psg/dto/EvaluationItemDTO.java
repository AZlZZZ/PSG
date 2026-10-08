package com.psg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationItemDTO {
    private Long id;
    private Long securityActionId;
    private String actionCode;
    private String isoControl;
    private String theme;
    private String projectSubdomain;
    private String description;
    private Integer coefficient;
    private Integer sectorDefaultCoefficient;
    private String implementationStatus;
    private String justification;
    private String comment;
}
