package com.psg.service;

import com.psg.dto.EvaluationScoreResult;
import com.psg.dto.EvaluationScoreResult.*;
import com.psg.model.*;
import com.psg.repository.ActionSectorCoefficientRepository;
import com.psg.repository.EvaluationRepository;
import com.psg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ScoringService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private ActionSectorCoefficientRepository coefficientRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Calculates the deterministic cybersecurity evaluation score.
     * Enforces strict authorization:
     * - ROLE_ADMIN can access any evaluation.
     * - FIRM_USER / ROLE_USER can only access evaluations belonging to their own organization.
     */
    @Transactional(readOnly = true)
    public EvaluationScoreResult calculateScore(Long evaluationId, String username) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation non trouvée avec l'identifiant: " + evaluationId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé: " + username));

        // Authorization check
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());
        if (!isAdmin) {
            if (user.getOrganization() == null || evaluation.getOrganization() == null ||
                    !user.getOrganization().getId().equals(evaluation.getOrganization().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous ne pouvez consulter que les évaluations de votre propre organisation.");
            }
        }

        Organization org = evaluation.getOrganization();
        if (org == null || org.getSector() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'organisation ou son secteur d'activité n'est pas défini pour cette évaluation.");
        }
        Sector sector = org.getSector();

        List<EvaluationItem> items = evaluation.getItems();
        if (items == null) {
            items = Collections.emptyList();
        }

        // Cache sector coefficients for quick lookup
        Map<Long, ActionSectorCoefficient> coeffMap = new HashMap<>();
        for (ActionSectorCoefficient asc : coefficientRepository.findBySector(sector)) {
            coeffMap.put(asc.getAction().getId(), asc);
        }

        double totalWeightedNumerator = 0.0;
        double totalDenominator = 0.0;
        int applicableCount = 0;
        int notApplicableCount = 0;

        // Groupings for Themes and Subdomains
        // Key: Theme name -> [numerator, denominator, applicableCount]
        Map<String, double[]> themeAggregates = new LinkedHashMap<>();
        // Key: Subdomain name -> [numerator, denominator, applicableCount]
        Map<String, double[]> subdomainAggregates = new LinkedHashMap<>();
        Map<String, String> subdomainToThemeMap = new LinkedHashMap<>();

        List<ActionScoreResult> actionResults = new ArrayList<>();

        for (EvaluationItem item : items) {
            SecurityAction action = item.getSecurityAction();
            ImplementationStatus status = item.getImplementationStatus();

            Integer coeff = item.getCoefficient();
            if (coeff == null) {
                ActionSectorCoefficient coeffObj = coeffMap.get(action.getId());
                if (coeffObj == null) {
                    coeffObj = coefficientRepository.findByActionAndSector(action, sector)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Coefficient sectoriel manquant pour l'action " + action.getCode() + " et le secteur " + sector.getName()));
                }
                coeff = coeffObj.getCoefficient();
            }

            if (coeff == null || coeff < 1 || coeff > 5) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Valeur de coefficient invalide (" + coeff + ") pour l'action " + action.getCode());
            }

            // Track themes and subdomains order
            String theme = action.getTheme();
            String subdomain = action.getProjectSubdomain();
            themeAggregates.putIfAbsent(theme, new double[]{0.0, 0.0, 0.0});
            subdomainAggregates.putIfAbsent(subdomain, new double[]{0.0, 0.0, 0.0});
            subdomainToThemeMap.putIfAbsent(subdomain, theme);

            if (status == ImplementationStatus.NOT_APPLICABLE) {
                notApplicableCount++;
                actionResults.add(ActionScoreResult.builder()
                        .actionCode(action.getCode())
                        .isoControl(action.getIsoControl())
                        .actionDescription(action.getDescription())
                        .coefficient(coeff)
                        .implementationStatus(status.name())
                        .implementationRate(null)
                        .weightedContribution(null)
                        .weaknessPriority(null)
                        .priorityLevel(null)
                        .build());
            } else {
                applicableCount++;
                double rate = getImplementationRate(status);
                double weightedContribution = coeff * rate;
                double priority = coeff * (1.0 - rate);
                String priorityLevel = getPriorityLevel(priority);

                totalWeightedNumerator += weightedContribution;
                totalDenominator += coeff;

                // Theme aggregate
                double[] themeStats = themeAggregates.get(theme);
                themeStats[0] += weightedContribution;
                themeStats[1] += coeff;
                themeStats[2] += 1.0;

                // Subdomain aggregate
                double[] subdomainStats = subdomainAggregates.get(subdomain);
                subdomainStats[0] += weightedContribution;
                subdomainStats[1] += coeff;
                subdomainStats[2] += 1.0;

                actionResults.add(ActionScoreResult.builder()
                        .actionCode(action.getCode())
                        .isoControl(action.getIsoControl())
                        .actionDescription(action.getDescription())
                        .coefficient(coeff)
                        .implementationStatus(status.name())
                        .implementationRate(rate)
                        .weightedContribution(roundToTwoDecimals(weightedContribution))
                        .weaknessPriority(roundToTwoDecimals(priority))
                        .priorityLevel(priorityLevel)
                        .build());
            }
        }

        // Sort action results by actionCode (e.g. A01, A02...)
        actionResults.sort(Comparator.comparing(ActionScoreResult::getActionCode));

        // Overall score calculation
        Double overallScore = null;
        String overallLevel;
        if (applicableCount == 0 || totalDenominator == 0.0) {
            overallLevel = "NO_APPLICABLE_ITEMS";
        } else {
            double rawScore = (totalWeightedNumerator / totalDenominator) * 100.0;
            overallScore = roundToTwoDecimals(rawScore);
            overallLevel = getScoreBand(overallScore);
        }

        // Build Theme results
        List<ThemeScoreResult> themeResults = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : themeAggregates.entrySet()) {
            String themeName = entry.getKey();
            double[] stats = entry.getValue();
            int tApplicable = (int) stats[2];
            Double tScore = null;
            String tLevel = "NO_APPLICABLE_ITEMS";

            if (tApplicable > 0 && stats[1] > 0.0) {
                double raw = (stats[0] / stats[1]) * 100.0;
                tScore = roundToTwoDecimals(raw);
                tLevel = getScoreBand(tScore);
            }

            themeResults.add(ThemeScoreResult.builder()
                    .theme(themeName)
                    .score(tScore)
                    .level(tLevel)
                    .applicableActionCount(tApplicable)
                    .build());
        }

        // Build Subdomain results
        List<SubdomainScoreResult> subdomainResults = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : subdomainAggregates.entrySet()) {
            String subName = entry.getKey();
            String themeName = subdomainToThemeMap.get(subName);
            double[] stats = entry.getValue();
            int sApplicable = (int) stats[2];
            Double sScore = null;
            String sLevel = "NO_APPLICABLE_ITEMS";

            if (sApplicable > 0 && stats[1] > 0.0) {
                double raw = (stats[0] / stats[1]) * 100.0;
                sScore = roundToTwoDecimals(raw);
                sLevel = getScoreBand(sScore);
            }

            subdomainResults.add(SubdomainScoreResult.builder()
                    .subdomain(subName)
                    .theme(themeName)
                    .score(sScore)
                    .level(sLevel)
                    .applicableActionCount(sApplicable)
                    .build());
        }

        return EvaluationScoreResult.builder()
                .evaluationId(evaluation.getId())
                .organizationName(org.getName())
                .sectorName(sector.getName())
                .overallScore(overallScore)
                .overallLevel(overallLevel)
                .applicableActionCount(applicableCount)
                .notApplicableActionCount(notApplicableCount)
                .themeScores(themeResults)
                .subdomainScores(subdomainResults)
                .actionResults(actionResults)
                .build();
    }

    /**
     * Deterministic implementation rate mappings:
     * IMPLEMENTED     -> 1.00 (100%)
     * IN_PROGRESS     -> 0.50 (50%)
     * PLANNED         -> 0.25 (25%)
     * NOT_PLANNED     -> 0.00 (0%)
     */
    public static double getImplementationRate(ImplementationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Le statut de mise en œuvre ne peut pas être nul.");
        }
        return switch (status) {
            case IMPLEMENTED -> 1.0;
            case IN_PROGRESS -> 0.5;
            case PLANNED -> 0.25;
            case NOT_PLANNED -> 0.0;
            case NOT_APPLICABLE -> throw new IllegalArgumentException("NOT_APPLICABLE n'a pas de taux de mise en œuvre applicable.");
        };
    }

    /**
     * Weakness priority level thresholds:
     * 0                -> "Aucun"
     * > 0 and < 2      -> "Faible"
     * >= 2 and < 3.5   -> "Moyenne"
     * >= 3.5 and < 4.5 -> "Élevée"
     * >= 4.5 and <= 5  -> "Critique"
     */
    public static String getPriorityLevel(double priority) {
        if (priority == 0.0) {
            return "Aucun";
        } else if (priority > 0.0 && priority < 2.0) {
            return "Faible";
        } else if (priority >= 2.0 && priority < 3.5) {
            return "Moyenne";
        } else if (priority >= 3.5 && priority < 4.5) {
            return "Élevée";
        } else if (priority >= 4.5 && priority <= 5.0001) {
            return "Critique";
        }
        return "Critique";
    }

    /**
     * Overall score bands:
     * < 20     -> "Critique"
     * 20 - <40 -> "Faible"
     * 40 - <60 -> "À améliorer"
     * 60 - <80 -> "Bon"
     * 80 - 100 -> "Très bon"
     */
    public static String getScoreBand(double score) {
        if (score < 20.0) {
            return "Critique";
        } else if (score < 40.0) {
            return "Faible";
        } else if (score < 60.0) {
            return "À améliorer";
        } else if (score < 80.0) {
            return "Bon";
        } else {
            return "Très bon";
        }
    }

    /**
     * Rounds value to exactly 2 decimal places using HALF_UP.
     */
    public static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
