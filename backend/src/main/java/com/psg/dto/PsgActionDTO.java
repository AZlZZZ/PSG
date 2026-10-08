package com.psg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PsgActionDTO {
    private Long id;
    private String code;
    private String isoControl;
    private String theme;
    private String projectSubdomain;
    private String description;
    private Integer coefficient;
    private Long sectorId;
    private String sectorName;
}
