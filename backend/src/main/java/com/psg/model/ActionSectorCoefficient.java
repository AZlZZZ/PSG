package com.psg.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "action_sector_coefficients",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_action_sector", columnNames = {"action_id", "sector_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionSectorCoefficient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "action_id", nullable = false)
    private SecurityAction action;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false)
    private Integer coefficient; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String justification;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
