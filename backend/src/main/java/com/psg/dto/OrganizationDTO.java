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
public class OrganizationDTO {
    private Long id;
    private String name;
    private Long sectorId;
    private String sectorName;
    private int userCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
