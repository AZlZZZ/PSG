package com.psg;

import com.psg.dto.EvaluationRequest;
import com.psg.model.*;
import com.psg.repository.EvaluationRepository;
import com.psg.repository.EvaluationItemRepository;
import com.psg.service.AdminManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminEvaluationManagementTest {

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationItemRepository evaluationItemRepository;

    @Test
    @DisplayName("Test 1 — ADMIN can list evaluations")
    void testListEvaluations() {
        var list = adminManagementService.getAllEvaluations();
        assertNotNull(list);
    }

    @Test
    @DisplayName("Test 2 — ADMIN can retrieve evaluation")
    void testRetrieveEvaluation() {
        var sectors = adminManagementService.getAllSectors();
        assertFalse(sectors.isEmpty());
        Long sectorId = sectors.get(0).getId();

        // Create organization
        var org = adminManagementService.createOrganization(
                com.psg.dto.OrganizationRequest.builder()
                        .name("Test Org " + System.currentTimeMillis())
                        .sectorId(sectorId)
                        .build());

        // Create evaluation
        var eval = adminManagementService.createEvaluation(
                EvaluationRequest.builder()
                        .organizationId(org.getId())
                        .build());

        var retrieved = adminManagementService.getEvaluationById(eval.getId());
        assertEquals(eval.getId(), retrieved.getId());
        assertEquals("DRAFT", retrieved.getStatus());
    }

    @Test
    @DisplayName("Test 3 — ADMIN can create evaluation")
    void testCreateEvaluation() {
        var sectors = adminManagementService.getAllSectors();
        assertFalse(sectors.isEmpty());
        Long sectorId = sectors.get(0).getId();

        var org = adminManagementService.createOrganization(
                com.psg.dto.OrganizationRequest.builder()
                        .name("Eval Test Org " + System.currentTimeMillis())
                        .sectorId(sectorId)
                        .build());

        var eval = adminManagementService.createEvaluation(
                EvaluationRequest.builder()
                        .organizationId(org.getId())
                        .build());

        assertNotNull(eval.getId());
        assertEquals("DRAFT", eval.getStatus());
        assertEquals(org.getId(), eval.getOrganizationId());
    }

    @Test
    @DisplayName("Test 4 — ADMIN can update evaluation status")
    void testUpdateEvaluationStatus() {
        var sectors = adminManagementService.getAllSectors();
        assertFalse(sectors.isEmpty());
        Long sectorId = sectors.get(0).getId();

        var org = adminManagementService.createOrganization(
                com.psg.dto.OrganizationRequest.builder()
                        .name("Status Test Org " + System.currentTimeMillis())
                        .sectorId(sectorId)
                        .build());

        var eval = adminManagementService.createEvaluation(
                EvaluationRequest.builder()
                        .organizationId(org.getId())
                        .build());

        var updated = adminManagementService.updateEvaluationStatus(eval.getId(),
                EvaluationRequest.builder()
                        .status("ASSIGNED")
                        .build());

        assertEquals("ASSIGNED", updated.getStatus());
    }

    @Test
    @DisplayName("Test 5 — Invalid organization rejected")
    void testInvalidOrganizationRejected() {
        EvaluationRequest request = EvaluationRequest.builder()
                .organizationId(999999L)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminManagementService.createEvaluation(request));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Test 6 — Invalid status rejected")
    void testInvalidStatusRejected() {
        var sectors = adminManagementService.getAllSectors();
        assertFalse(sectors.isEmpty());
        Long sectorId = sectors.get(0).getId();

        var org = adminManagementService.createOrganization(
                com.psg.dto.OrganizationRequest.builder()
                        .name("Invalid Status Org " + System.currentTimeMillis())
                        .sectorId(sectorId)
                        .build());

        var eval = adminManagementService.createEvaluation(
                EvaluationRequest.builder()
                        .organizationId(org.getId())
                        .build());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminManagementService.updateEvaluationStatus(eval.getId(),
                        EvaluationRequest.builder()
                                .status("INVALID_STATUS")
                                .build()));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Test 7 — ADMIN can delete evaluation without items")
    void testDeleteEvaluationWithoutItems() {
        var sectors = adminManagementService.getAllSectors();
        assertFalse(sectors.isEmpty());
        Long sectorId = sectors.get(0).getId();

        var org = adminManagementService.createOrganization(
                com.psg.dto.OrganizationRequest.builder()
                        .name("Delete Org " + System.currentTimeMillis())
                        .sectorId(sectorId)
                        .build());

        var eval = adminManagementService.createEvaluation(
                EvaluationRequest.builder()
                        .organizationId(org.getId())
                        .build());

        // Delete should succeed for evaluation without items
        assertDoesNotThrow(() -> adminManagementService.deleteEvaluation(eval.getId()));
    }
}