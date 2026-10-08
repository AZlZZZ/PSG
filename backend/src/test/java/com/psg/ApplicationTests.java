package com.psg;

import com.psg.repository.ActionSectorCoefficientRepository;
import com.psg.repository.SectorRepository;
import com.psg.repository.SecurityActionRepository;
import com.psg.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplicationTests {

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SecurityActionRepository securityActionRepository;

    @Autowired
    private ActionSectorCoefficientRepository coefficientRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoadsAndSeedingIsAccurate() {
        // Verify exact counts
        assertEquals(9, sectorRepository.count(), "There must be exactly 9 sectors");
        assertEquals(93, securityActionRepository.count(), "There must be exactly 93 security actions");
        assertEquals(837, coefficientRepository.count(), "There must be exactly 837 action-sector coefficients");

        // Verify users are preserved
        assertTrue(userRepository.findByUsername("admin").isPresent(), "admin user must exist");
        assertTrue(userRepository.findByUsername("firm1").isPresent(), "firm1 user must exist");

        // Verify specific data points from the authoritative matrix
        var a01 = securityActionRepository.findByCode("A01").orElseThrow();
        assertEquals("ISO 5.1", a01.getIsoControl());
        assertEquals("Organizational", a01.getTheme());
        assertEquals("Governance & Security Policies", a01.getProjectSubdomain());

        var santeSector = sectorRepository.findByName("Santé").orElseThrow();
        var a01Sante = coefficientRepository.findByActionAndSector(a01, santeSector).orElseThrow();
        assertEquals(5, a01Sante.getCoefficient());

        var a93 = securityActionRepository.findByCode("A93").orElseThrow();
        var agricultureSector = sectorRepository.findByName("Agriculture").orElseThrow();
        var a93Agri = coefficientRepository.findByActionAndSector(a93, agricultureSector).orElseThrow();
        assertEquals(3, a93Agri.getCoefficient());
    }
}
