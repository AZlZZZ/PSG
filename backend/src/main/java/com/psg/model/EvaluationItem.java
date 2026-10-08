package com.psg.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluation_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_evaluation_action", columnNames = {"evaluation_id", "security_action_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    @JsonIgnore
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "security_action_id", nullable = false)
    private SecurityAction securityAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImplementationStatus implementationStatus;

    @Column(columnDefinition = "TEXT")
    private String justification; // Mandatory when implementationStatus == NOT_APPLICABLE

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "coefficient")
    private Integer coefficient;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
