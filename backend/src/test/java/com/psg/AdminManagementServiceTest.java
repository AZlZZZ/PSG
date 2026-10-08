package com.psg;

import com.psg.dto.FirmUserRequest;
import com.psg.dto.OrganizationRequest;
import com.psg.model.*;
import com.psg.repository.EvaluationRepository;
import com.psg.repository.OrganizationRepository;
import com.psg.repository.SectorRepository;
import com.psg.repository.UserRepository;
import com.psg.service.AdminManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminManagementServiceTest {

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Test 1 — ADMIN can create organization")
    void testCreateOrganization() {
        Sector sector = sectorRepository.findByName("Finance").orElseThrow();
        OrganizationRequest request = OrganizationRequest.builder()
                .name("Nouvelle Banque 2026")
                .sectorId(sector.getId())
                .build();

        var res = adminManagementService.createOrganization(request);
        assertNotNull(res.getId());
        assertEquals("Nouvelle Banque 2026", res.getName());
        assertEquals("Finance", res.getSectorName());
    }

    @Test
    @DisplayName("Test 2b — ADMIN can retrieve organization")
    void testRetrieveOrganization() {
        Sector sector = sectorRepository.findByName("Finance").orElseThrow();
        Organization org = organizationRepository.save(Organization.builder()
                .name("Banque Retrieve Test")
                .sector(sector)
                .build());

        var res = adminManagementService.getOrganizationById(org.getId());
        assertEquals(org.getId(), res.getId());
        assertEquals("Banque Retrieve Test", res.getName());
        assertEquals("Finance", res.getSectorName());
        assertEquals(sector.getId(), res.getSectorId());
        assertNotNull(res.getCreatedAt());
    }

    @Test
    @DisplayName("Test 2 — ADMIN can update organization")
    void testUpdateOrganization() {
        Sector sante = sectorRepository.findByName("Santé").orElseThrow();
        Sector finance = sectorRepository.findByName("Finance").orElseThrow();

        Organization org = organizationRepository.save(Organization.builder()
                .name("Hopital Central")
                .sector(sante)
                .build());

        OrganizationRequest updateReq = OrganizationRequest.builder()
                .name("Hopital Central Modifie")
                .sectorId(finance.getId())
                .build();

        var res = adminManagementService.updateOrganization(org.getId(), updateReq);
        assertEquals("Hopital Central Modifie", res.getName());
        assertEquals("Finance", res.getSectorName());
    }

    @Test
    @DisplayName("Test 3 — ADMIN can list organizations")
    void testListOrganizations() {
        var list = adminManagementService.getAllOrganizations();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    @DisplayName("Test 4 — ADMIN can create firm user")
    void testCreateFirmUser() {
        Sector sector = sectorRepository.findByName("Santé").orElseThrow();
        Organization org = organizationRepository.save(Organization.builder()
                .name("Pharmacie Centrale Test")
                .sector(sector)
                .build());

        FirmUserRequest request = FirmUserRequest.builder()
                .username("pharmacien_test")
                .password("secret123")
                .organizationId(org.getId())
                .build();

        var res = adminManagementService.createFirmUser(request);
        assertNotNull(res.getId());
        assertEquals("pharmacien_test", res.getUsername());
        assertEquals("ROLE_USER", res.getRole());
        assertEquals(org.getId(), res.getOrganizationId());
    }

    @Test
    @DisplayName("Test 5 — Created firm user's password is BCrypt encoded")
    void testPasswordIsBcryptEncoded() {
        Sector sector = sectorRepository.findByName("Santé").orElseThrow();
        Organization org = organizationRepository.save(Organization.builder()
                .name("Labo Test BCrypt")
                .sector(sector)
                .build());

        FirmUserRequest request = FirmUserRequest.builder()
                .username("bcrypt_user_test")
                .password("monMotDePasse")
                .organizationId(org.getId())
                .build();

        var res = adminManagementService.createFirmUser(request);
        User user = userRepository.findById(res.getId()).orElseThrow();

        assertNotEquals("monMotDePasse", user.getPassword());
        assertTrue(passwordEncoder.matches("monMotDePasse", user.getPassword()));
    }

    @Test
    @DisplayName("Test 6 — Created firm user is linked to the correct organization")
    void testFirmUserLinkedToCorrectOrganization() {
        Sector sector = sectorRepository.findByName("Transport").orElseThrow();
        Organization org = organizationRepository.save(Organization.builder()
                .name("Compagnie Express")
                .sector(sector)
                .build());

        FirmUserRequest request = FirmUserRequest.builder()
                .username("chauffeur_test")
                .password("pwd456")
                .organizationId(org.getId())
                .build();

        var res = adminManagementService.createFirmUser(request);
        assertEquals(org.getId(), res.getOrganizationId());
        assertEquals("Compagnie Express", res.getOrganizationName());
        assertEquals("Transport", res.getSectorName());
    }

    @Test
    @DisplayName("Test 7 — Duplicate organization is rejected")
    void testDuplicateOrganizationRejected() {
        Sector sector = sectorRepository.findByName("Énergie").orElseThrow();
        organizationRepository.save(Organization.builder()
                .name("Energie Solaire SA")
                .sector(sector)
                .build());

        OrganizationRequest duplicateReq = OrganizationRequest.builder()
                .name("Energie Solaire SA")
                .sectorId(sector.getId())
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminManagementService.createOrganization(duplicateReq));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Test 8 — Duplicate username is rejected")
    void testDuplicateUsernameRejected() {
        Sector sector = sectorRepository.findByName("Commerce").orElseThrow();
        Organization org = organizationRepository.save(Organization.builder()
                .name("Supermarché Test")
                .sector(sector)
                .build());

        adminManagementService.createFirmUser(FirmUserRequest.builder()
                .username("caissier_unique")
                .password("pwd1")
                .organizationId(org.getId())
                .build());

        FirmUserRequest dupReq = FirmUserRequest.builder()
                .username("caissier_unique")
                .password("pwd2")
                .organizationId(org.getId())
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminManagementService.createFirmUser(dupReq));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Test 11 — Organization deletion does not destroy evaluation history (rejected with 409)")
    void testOrganizationDeletionBlockedWhenEvaluationsExist() {
        Sector sector = sectorRepository.findByName("Agriculture").orElseThrow();
        Organization org = organizationRepository.save(Organization.builder()
                .name("Coopérative Ble")
                .sector(sector)
                .build());

        Evaluation evaluation = evaluationRepository.save(Evaluation.builder()
                .organization(org)
                .status(EvaluationStatus.IN_PROGRESS)
                .build());

        Long orgId = org.getId();
        Long evalId = evaluation.getId();
        long evaluationCountBefore = evaluationRepository.count();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminManagementService.deleteOrganization(orgId));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getMessage().contains("évaluation"));

        assertTrue(organizationRepository.existsById(orgId), "Organisation must still exist");
        assertTrue(evaluationRepository.findById(evalId).isPresent(), "Evaluation history must be preserved");
        assertEquals(evaluationCountBefore, evaluationRepository.count());
    }

    @Test
    @DisplayName("Test 12 — Invalid sector is rejected")
    void testInvalidSectorRejected() {
        OrganizationRequest invalidReq = OrganizationRequest.builder()
                .name("Entreprise Sans Secteur")
                .sectorId(999999L)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminManagementService.createOrganization(invalidReq));
        assertEquals(400, ex.getStatusCode().value());
    }
}
