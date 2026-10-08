package com.psg;

import com.psg.dto.EvaluationScoreResult;
import com.psg.model.*;
import com.psg.repository.*;
import com.psg.service.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ScoringServiceTest {

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SecurityActionRepository securityActionRepository;

    @Autowired
    private ActionSectorCoefficientRepository coefficientRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Sector santeSector;
    private Organization orgSante;
    private User firmUserSante;
    private User otherFirmUser;

    @BeforeEach
    void setUp() {
        santeSector = sectorRepository.findByName("Santé").orElseThrow();

        orgSante = organizationRepository.findByName("Clinique Espoir")
                .orElseGet(() -> organizationRepository.save(
                        Organization.builder()
                                .name("Clinique Espoir")
                                .sector(santeSector)
                                .build()
                ));

        firmUserSante = userRepository.findByUsername("firm_sante")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("firm_sante")
                                .password(passwordEncoder.encode("pass123"))
                                .role("ROLE_USER")
                                .organization(orgSante)
                                .build()
                ));

        Sector financeSector = sectorRepository.findByName("Finance").orElseThrow();
        Organization orgFinance = organizationRepository.findByName("Banque Nationale")
                .orElseGet(() -> organizationRepository.save(
                        Organization.builder()
                                .name("Banque Nationale")
                                .sector(financeSector)
                                .build()
                ));

        otherFirmUser = userRepository.findByUsername("firm_other")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username("firm_other")
                                .password(passwordEncoder.encode("pass123"))
                                .role("ROLE_USER")
                                .organization(orgFinance)
                                .build()
                ));
    }

    private Evaluation createEvaluationWithItems(List<EvaluationItemDef> defs) {
        Evaluation eval = Evaluation.builder()
                .organization(orgSante)
                .status(EvaluationStatus.IN_PROGRESS)
                .items(new ArrayList<>())
                .build();
        eval = evaluationRepository.save(eval);

        for (EvaluationItemDef def : defs) {
            SecurityAction action = securityActionRepository.findByCode(def.actionCode).orElseThrow();
            EvaluationItem item = EvaluationItem.builder()
                    .evaluation(eval)
                    .securityAction(action)
                    .implementationStatus(def.status)
                    .justification(def.status == ImplementationStatus.NOT_APPLICABLE ? "Non applicable au contexte" : null)
                    .build();
            eval.getItems().add(item);
        }

        return evaluationRepository.save(eval);
    }

    private record EvaluationItemDef(String actionCode, ImplementationStatus status) {}

    @Test
    @DisplayName("Test 1 — All implemented: score must equal exactly 100%")
    void testAllImplemented() {
        // A01 (coeff 5), A02 (coeff 5), A03 (coeff 4) for Santé
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.IMPLEMENTED),
                new EvaluationItemDef("A02", ImplementationStatus.IMPLEMENTED),
                new EvaluationItemDef("A03", ImplementationStatus.IMPLEMENTED)
        ));

        EvaluationScoreResult result = scoringService.calculateScore(eval.getId(), "admin");
        assertEquals(100.0, result.getOverallScore());
        assertEquals("Très bon", result.getOverallLevel());
        assertEquals(3, result.getApplicableActionCount());
        assertEquals(0, result.getNotApplicableActionCount());
    }

    @Test
    @DisplayName("Test 2 — All not planned: score must equal exactly 0%")
    void testAllNotPlanned() {
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.NOT_PLANNED),
                new EvaluationItemDef("A02", ImplementationStatus.NOT_PLANNED),
                new EvaluationItemDef("A03", ImplementationStatus.NOT_PLANNED)
        ));

        EvaluationScoreResult result = scoringService.calculateScore(eval.getId(), "admin");
        assertEquals(0.0, result.getOverallScore());
        assertEquals("Critique", result.getOverallLevel());
        assertEquals(3, result.getApplicableActionCount());
    }

    @Test
    @DisplayName("Test 3 — Mixed statuses: exact formula match")
    void testMixedStatuses() {
        // Santé coefficients:
        // A01: coeff = 5, status = IMPLEMENTED (1.00) -> 5 * 1.00 = 5.0
        // A02: coeff = 5, status = IN_PROGRESS (0.50) -> 5 * 0.50 = 2.5
        // A03: coeff = 4, status = PLANNED (0.25)     -> 4 * 0.25 = 1.0
        // A04: coeff = 5, status = NOT_PLANNED (0.00) -> 5 * 0.00 = 0.0
        // Numerator = 5.0 + 2.5 + 1.0 + 0.0 = 8.5
        // Denominator = 5 + 5 + 4 + 5 = 19
        // Score = (8.5 / 19) * 100 = 44.7368... -> rounded to 44.74
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.IMPLEMENTED),
                new EvaluationItemDef("A02", ImplementationStatus.IN_PROGRESS),
                new EvaluationItemDef("A03", ImplementationStatus.PLANNED),
                new EvaluationItemDef("A04", ImplementationStatus.NOT_PLANNED)
        ));

        EvaluationScoreResult result = scoringService.calculateScore(eval.getId(), "admin");
        assertEquals(44.74, result.getOverallScore());
        assertEquals("À améliorer", result.getOverallLevel());
        assertEquals(4, result.getApplicableActionCount());
    }

    @Test
    @DisplayName("Test 4 — N/A exclusion: excluded from both numerator and denominator")
    void testNotApplicableExclusion() {
        // A01 (coeff 5, IMPLEMENTED = 1.0) -> num: 5.0, den: 5
        // A02 (coeff 5, NOT_APPLICABLE)   -> excluded!
        // A03 (coeff 4, NOT_PLANNED = 0.0) -> num: 0.0, den: 4
        // Expected Score: (5.0 / 9) * 100 = 55.555... -> 55.56
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.IMPLEMENTED),
                new EvaluationItemDef("A02", ImplementationStatus.NOT_APPLICABLE),
                new EvaluationItemDef("A03", ImplementationStatus.NOT_PLANNED)
        ));

        EvaluationScoreResult result = scoringService.calculateScore(eval.getId(), "admin");
        assertEquals(55.56, result.getOverallScore());
        assertEquals(2, result.getApplicableActionCount());
        assertEquals(1, result.getNotApplicableActionCount());
    }

    @Test
    @DisplayName("Test 5 — All N/A: returns null score and NO_APPLICABLE_ITEMS")
    void testAllNotApplicable() {
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.NOT_APPLICABLE),
                new EvaluationItemDef("A02", ImplementationStatus.NOT_APPLICABLE)
        ));

        EvaluationScoreResult result = scoringService.calculateScore(eval.getId(), "admin");
        assertNull(result.getOverallScore());
        assertEquals("NO_APPLICABLE_ITEMS", result.getOverallLevel());
        assertEquals(0, result.getApplicableActionCount());
        assertEquals(2, result.getNotApplicableActionCount());
    }

    @Test
    @DisplayName("Test 6 — Weakness priority: C_i * (1 - I_i)")
    void testWeaknessPriority() {
        // A01: coeff = 5, IMPLEMENTED (1.0) -> priority = 5 * (1 - 1) = 0.0
        // A02: coeff = 5, NOT_PLANNED (0.0) -> priority = 5 * (1 - 0) = 5.0
        // A03: coeff = 4, IN_PROGRESS (0.5) -> priority = 4 * (1 - 0.5) = 2.0
        // A06: coeff = 4, PLANNED (0.25)     -> priority = 4 * (1 - 0.25) = 3.0
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.IMPLEMENTED),
                new EvaluationItemDef("A02", ImplementationStatus.NOT_PLANNED),
                new EvaluationItemDef("A03", ImplementationStatus.IN_PROGRESS),
                new EvaluationItemDef("A06", ImplementationStatus.PLANNED)
        ));

        EvaluationScoreResult result = scoringService.calculateScore(eval.getId(), "admin");

        var a01Res = result.getActionResults().stream().filter(a -> a.getActionCode().equals("A01")).findFirst().orElseThrow();
        assertEquals(0.0, a01Res.getWeaknessPriority());
        assertEquals("Aucun", a01Res.getPriorityLevel());

        var a02Res = result.getActionResults().stream().filter(a -> a.getActionCode().equals("A02")).findFirst().orElseThrow();
        assertEquals(5.0, a02Res.getWeaknessPriority());
        assertEquals("Critique", a02Res.getPriorityLevel());

        var a03Res = result.getActionResults().stream().filter(a -> a.getActionCode().equals("A03")).findFirst().orElseThrow();
        assertEquals(2.0, a03Res.getWeaknessPriority());
        assertEquals("Moyenne", a03Res.getPriorityLevel());

        var a06Res = result.getActionResults().stream().filter(a -> a.getActionCode().equals("A06")).findFirst().orElseThrow();
        assertEquals(3.0, a06Res.getWeaknessPriority());
        assertEquals("Moyenne", a06Res.getPriorityLevel());
    }

    @Test
    @DisplayName("Test 7 — Priority level boundary thresholds")
    void testPriorityLevelBoundaries() {
        assertEquals("Aucun", ScoringService.getPriorityLevel(0.0));
        assertEquals("Faible", ScoringService.getPriorityLevel(0.01));
        assertEquals("Faible", ScoringService.getPriorityLevel(1.99));
        assertEquals("Moyenne", ScoringService.getPriorityLevel(2.0));
        assertEquals("Moyenne", ScoringService.getPriorityLevel(3.49));
        assertEquals("Élevée", ScoringService.getPriorityLevel(3.5));
        assertEquals("Élevée", ScoringService.getPriorityLevel(4.49));
        assertEquals("Critique", ScoringService.getPriorityLevel(4.5));
        assertEquals("Critique", ScoringService.getPriorityLevel(5.0));
    }

    @Test
    @DisplayName("Test 8 — Missing coefficient produces an explicit error")
    void testMissingCoefficientError() {
        // Create an un-seeded dummy action
        SecurityAction dummyAction = securityActionRepository.save(
                SecurityAction.builder()
                        .code("A99")
                        .isoControl("ISO 99.9")
                        .theme("Technological")
                        .projectSubdomain("Testing")
                        .description("Action test sans coefficient")
                        .build()
        );

        Evaluation eval = Evaluation.builder()
                .organization(orgSante)
                .status(EvaluationStatus.IN_PROGRESS)
                .items(new ArrayList<>())
                .build();
        eval.getItems().add(EvaluationItem.builder()
                .evaluation(eval)
                .securityAction(dummyAction)
                .implementationStatus(ImplementationStatus.IMPLEMENTED)
                .build());
        eval = evaluationRepository.save(eval);

        Long evalId = eval.getId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                scoringService.calculateScore(evalId, "admin")
        );
        assertTrue(exception.getMessage().contains("Coefficient sectoriel manquant"));
    }

    @Test
    @DisplayName("Test 9 — Authorization: FIRM_USER cannot access another organization's score")
    void testAuthorizationIsolation() {
        Evaluation eval = createEvaluationWithItems(List.of(
                new EvaluationItemDef("A01", ImplementationStatus.IMPLEMENTED)
        ));

        // Owner firm user can access
        assertDoesNotThrow(() -> scoringService.calculateScore(eval.getId(), firmUserSante.getUsername()));

        // Admin can access
        assertDoesNotThrow(() -> scoringService.calculateScore(eval.getId(), "admin"));

        // Other organization firm user gets 403 Forbidden
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                scoringService.calculateScore(eval.getId(), otherFirmUser.getUsername())
        );
        assertEquals(403, ex.getStatusCode().value());
    }
}
