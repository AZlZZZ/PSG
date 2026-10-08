package com.psg.controller;

import com.psg.dto.*;
import com.psg.service.AdminManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminManagementService adminManagementService;

    // --- SECTORS ---

    @GetMapping("/sectors")
    public ResponseEntity<List<SectorDTO>> getAllSectors() {
        return ResponseEntity.ok(adminManagementService.getAllSectors());
    }

    // --- ORGANIZATIONS ---

    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations() {
        return ResponseEntity.ok(adminManagementService.getAllOrganizations());
    }

    @GetMapping("/organizations/{id}")
    public ResponseEntity<OrganizationDTO> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(adminManagementService.getOrganizationById(id));
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationDTO> createOrganization(@RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(adminManagementService.createOrganization(request));
    }

    @PutMapping("/organizations/{id}")
    public ResponseEntity<OrganizationDTO> updateOrganization(
            @PathVariable Long id,
            @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(adminManagementService.updateOrganization(id, request));
    }

    @DeleteMapping("/organizations/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        adminManagementService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }

    // --- FIRM USERS ---

    @GetMapping("/firm-users")
    public ResponseEntity<List<FirmUserDTO>> getAllFirmUsers() {
        return ResponseEntity.ok(adminManagementService.getAllFirmUsers());
    }

    @GetMapping("/firm-users/{id}")
    public ResponseEntity<FirmUserDTO> getFirmUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminManagementService.getFirmUserById(id));
    }

    @PostMapping("/firm-users")
    public ResponseEntity<FirmUserDTO> createFirmUser(@RequestBody FirmUserRequest request) {
        return ResponseEntity.ok(adminManagementService.createFirmUser(request));
    }

    @PutMapping("/firm-users/{id}")
    public ResponseEntity<FirmUserDTO> updateFirmUser(
            @PathVariable Long id,
            @RequestBody FirmUserRequest request) {
        return ResponseEntity.ok(adminManagementService.updateFirmUser(id, request));
    }

    @DeleteMapping("/firm-users/{id}")
    public ResponseEntity<?> deleteFirmUser(@PathVariable Long id) {
        adminManagementService.deleteFirmUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- PSG REFERENCE CATALOG ---

    @GetMapping("/sectors/{sectorId}/psg-actions")
    public ResponseEntity<List<PsgActionDTO>> getPsgActionsBySector(@PathVariable Long sectorId) {
        return ResponseEntity.ok(adminManagementService.getPsgActionsBySector(sectorId));
    }

    @GetMapping("/psg-actions")
    public ResponseEntity<List<PsgActionDTO>> getAllPsgActions(@RequestParam(required = false) Long sectorId) {
        return ResponseEntity.ok(adminManagementService.getPsgActionsBySector(sectorId));
    }
}
