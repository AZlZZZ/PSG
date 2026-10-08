package com.psg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationScoreResult {

    private Long evaluationId;
    private String organizationName;
    private String sectorName;

    // Overall Score (null if NO_APPLICABLE_ITEMS)
    private Double overallScore;
    private String overallLevel; // "Critique", "Faible", "À améliorer", "Bon", "Très bon", or "NO_APPLICABLE_ITEMS"

    private int applicableActionCount;
    private int notApplicableActionCount;

    private List<ThemeScoreResult> themeScores;
    private List<SubdomainScoreResult> subdomainScores;
    private List<ActionScoreResult> actionResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThemeScoreResult {
        private String theme;
        private Double score; // null if no applicable items in theme
        private String level;
        private int applicableActionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubdomainScoreResult {
        private String subdomain;
        private String theme;
        private Double score; // null if no applicable items in subdomain
        private String level;
        private int applicableActionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionScoreResult {
        private String actionCode;
        private String isoControl;
        private String actionDescription;
        private Integer coefficient;
        private String implementationStatus;
        private Double implementationRate;      // null when NOT_APPLICABLE
        private Double weightedContribution;     // null when NOT_APPLICABLE
        private Double weaknessPriority;        // null when NOT_APPLICABLE
        private String priorityLevel;           // "Aucun", "Faible", "Moyenne", "Élevée", "Critique", or null
    }
}
