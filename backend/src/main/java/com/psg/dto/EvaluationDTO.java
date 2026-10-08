package com.psg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationDTO {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long sectorId;
    private String sectorName;
    private String status;
    private int itemCount;
    private java.util.List<EvaluationItemDTO> items;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}