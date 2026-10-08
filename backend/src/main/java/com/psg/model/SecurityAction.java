package com.psg.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String code; // e.g., "A01" to "A93"

    @Column(nullable = false, length = 50)
    private String isoControl; // e.g., "ISO 5.1"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String theme; // "Organizational", "People", "Physical", "Technological"

    @Column(nullable = false, length = 100)
    private String projectSubdomain; // e.g., "Governance & Security Policies"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
