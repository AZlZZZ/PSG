package com.psg.controller;

import com.psg.dto.*;
import com.psg.service.AdminManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/evaluations")
@PreAuthorize("hasRole('ADMIN')")
public class EvaluationAdminController {

    @Autowired
    private AdminManagementService adminManagementService;

    @GetMapping
    public ResponseEntity<List<EvaluationDTO>> getAllEvaluations() {
        return ResponseEntity.ok(adminManagementService.getAllEvaluations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationDTO> getEvaluationById(@PathVariable Long id) {
        return ResponseEntity.ok(adminManagementService.getEvaluationById(id));
    }

    @PostMapping
    public ResponseEntity<EvaluationDTO> createEvaluation(@RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(adminManagementService.createEvaluation(request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<EvaluationDTO> updateEvaluationStatus(
            @PathVariable Long id,
            @RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(adminManagementService.updateEvaluationStatus(id, request));
    }

    @PutMapping("/{id}/items/{itemId}/coefficient")
    public ResponseEntity<EvaluationDTO> updateItemCoefficient(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody java.util.Map<String, Integer> payload) {
        Integer newCoeff = payload.get("coefficient");
        return ResponseEntity.ok(adminManagementService.updateItemCoefficient(id, itemId, newCoeff));
    }

    @PutMapping("/{id}/items/{itemId}/coefficient/reset")
    public ResponseEntity<EvaluationDTO> resetItemCoefficient(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(adminManagementService.resetItemCoefficientToSectorDefault(id, itemId));
    }

    @Autowired
    private com.psg.service.ScoringService scoringService;

    @GetMapping("/{id}/score")
    public ResponseEntity<com.psg.dto.EvaluationScoreResult> getEvaluationScore(
            @PathVariable Long id,
            java.security.Principal principal) {
        return ResponseEntity.ok(scoringService.calculateScore(id, principal.getName()));
    }

    @PutMapping("/{id}/validate")
    public ResponseEntity<EvaluationDTO> validateEvaluation(@PathVariable Long id) {
        return ResponseEntity.ok(adminManagementService.validateEvaluation(id));
    }

    @PutMapping("/{id}/request-revision")
    public ResponseEntity<EvaluationDTO> requestRevision(@PathVariable Long id) {
        return ResponseEntity.ok(adminManagementService.returnEvaluationForRevision(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvaluation(@PathVariable Long id) {
        adminManagementService.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }
}