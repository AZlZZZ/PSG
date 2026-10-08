package com.psg.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "evaluation_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedUser;

    private LocalDate deadline;

    @Column(nullable = false)
    private String status; // "CREATED", "SUBMITTED"

    // Evaluation fields to be filled by the user
    private String physicalSecurityStatus; // "COMPLIANT", "PARTIALLY_COMPLIANT", "NON_COMPLIANT"
    private String networkSecurityStatus;  // "COMPLIANT", "PARTIALLY_COMPLIANT", "NON_COMPLIANT"
    private String dataProtectionStatus;   // "COMPLIANT", "PARTIALLY_COMPLIANT", "NON_COMPLIANT"
    
    @Column(columnDefinition = "TEXT")
    private String comments;
}
