package com.psg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirmUserDTO {
    private Long id;
    private String username;
    private String role;
    private Long organizationId;
    private String organizationName;
    private String sectorName;
}
