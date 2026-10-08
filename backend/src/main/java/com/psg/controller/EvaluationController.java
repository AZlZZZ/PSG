package com.psg.controller;

import com.psg.dto.EvaluationDTO;
import com.psg.dto.EvaluationScoreResult;
import com.psg.model.*;
import com.psg.repository.EvaluationItemRepository;
import com.psg.repository.EvaluationRepository;
import com.psg.repository.OrganizationRepository;
import com.psg.repository.UserRepository;
import com.psg.service.AdminManagementService;
import com.psg.service.ScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationItemRepository evaluationItemRepository;

    private Organization resolveUserOrganization(User user) {
        if (user.getOrganization() != null) {
            return user.getOrganization();
        }
        if (user.getOrganizationName() != null && !user.getOrganizationName().isBlank()) {
            return organizationRepository.findByName(user.getOrganizationName().trim()).orElse(null);
        }
        return null;
    }

    /**
     * Retrieves the assigned evaluation for the firm user's organization.
     */
    @GetMapping("/my-assigned")
    @Transactional
    public ResponseEntity<EvaluationDTO> getMyAssignedEvaluation(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));

        Organization userOrg = resolveUserOrganization(user);
        if (userOrg == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune organisation associée à cet utilisateur.");
        }

        List<Evaluation> evals = evaluationRepository.findByOrganization(userOrg);

        // Prioritize active evaluations (ASSIGNED or IN_PROGRESS) by highest ID (most recent)
        Evaluation assigned = evals.stream()
                .filter(e -> e.getStatus() == EvaluationStatus.ASSIGNED || e.getStatus() == EvaluationStatus.IN_PROGRESS)
                .max(Comparator.comparing(Evaluation::getId))
                .orElseGet(() -> evals.stream()
                        .filter(e -> e.getStatus() != EvaluationStatus.DRAFT)
                        .max(Comparator.comparing(Evaluation::getId))
                        .orElse(null)
                );

        if (assigned == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune évaluation assignée pour votre organisation.");
        }

        adminManagementService.ensureEvaluationItems(assigned);

        return ResponseEntity.ok(adminManagementService.getEvaluationById(assigned.getId()));
    }

    /**
     * Updates an evaluation item response for the firm user's organization.
     */
    @PutMapping("/{evaluationId}/items/{itemId}")
    public ResponseEntity<EvaluationDTO> submitItemResponse(
            @PathVariable Long evaluationId,
            @PathVariable Long itemId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));

        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée"));

        Organization userOrg = resolveUserOrganization(user);
        if (userOrg == null || eval.getOrganization() == null || !userOrg.getId().equals(eval.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        EvaluationItem item = evaluationItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément d'évaluation non trouvé"));

        if (payload.containsKey("implementationStatus")) {
            String statusStr = payload.get("implementationStatus");
            item.setImplementationStatus(ImplementationStatus.valueOf(statusStr));
        }
        if (payload.containsKey("justification")) {
            item.setJustification(payload.get("justification"));
        }
        if (payload.containsKey("comment")) {
            item.setComment(payload.get("comment"));
        }

        if (eval.getStatus() == EvaluationStatus.ASSIGNED) {
            eval.setStatus(EvaluationStatus.IN_PROGRESS);
            evaluationRepository.save(eval);
        }

        evaluationItemRepository.save(item);

        return ResponseEntity.ok(adminManagementService.getEvaluationById(evaluationId));
    }

    /**
     * Finalizes and submits the evaluation.
     */
    @PostMapping("/{evaluationId}/submit")
    public ResponseEntity<EvaluationDTO> submitEvaluation(
            @PathVariable Long evaluationId,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));

        Evaluation eval = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée"));

        Organization userOrg = resolveUserOrganization(user);
        if (userOrg == null || eval.getOrganization() == null || !userOrg.getId().equals(eval.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
        }

        eval.setStatus(EvaluationStatus.SUBMITTED);
        eval.setSubmittedAt(LocalDateTime.now());
        evaluationRepository.save(eval);

        return ResponseEntity.ok(adminManagementService.getEvaluationById(evaluationId));
    }

    /**
     * Retrieves the deterministic cybersecurity posture score for an evaluation.
     * Enforces organization authorization via the authenticated principal.
     */
    @GetMapping("/{evaluationId}/score")
    public ResponseEntity<EvaluationScoreResult> getEvaluationScore(
            @PathVariable Long evaluationId,
            Principal principal) {
        EvaluationScoreResult result = scoringService.calculateScore(evaluationId, principal.getName());
        return ResponseEntity.ok(result);
    }
}
