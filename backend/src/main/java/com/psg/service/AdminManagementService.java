package com.psg.service;

import com.psg.dto.*;
import com.psg.model.*;
import com.psg.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static com.psg.model.EvaluationStatus.*;

@Service
public class AdminManagementService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationItemRepository evaluationItemRepository;

    @Autowired
    private EvaluationProjectRepository evaluationProjectRepository;

    @Autowired
    private SecurityActionRepository securityActionRepository;

    @Autowired
    private ActionSectorCoefficientRepository coefficientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- SECTOR OPERATIONS ---

    @Transactional(readOnly = true)
    public List<SectorDTO> getAllSectors() {
        return sectorRepository.findAll().stream()
                .map(s -> SectorDTO.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    // --- ORGANIZATION OPERATIONS ---

    @Transactional(readOnly = true)
    public List<OrganizationDTO> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::toOrganizationDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDTO getOrganizationById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée avec l'identifiant: " + id));
        return toOrganizationDTO(org);
    }

    @Transactional
    public OrganizationDTO createOrganization(OrganizationRequest request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de l'organisation est obligatoire.");
        }
        if (request.getSectorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le secteur d'activité est obligatoire.");
        }

        String trimmedName = request.getName().trim();
        if (organizationRepository.existsByName(trimmedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une organisation avec ce nom existe déjà.");
        }

        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secteur invalide avec l'identifiant: " + request.getSectorId()));

        Organization org = Organization.builder()
                .name(trimmedName)
                .sector(sector)
                .build();

        return toOrganizationDTO(organizationRepository.save(org));
    }

    @Transactional
    public OrganizationDTO updateOrganization(Long id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée avec l'identifiant: " + id));

        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de l'organisation est obligatoire.");
        }
        if (request.getSectorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le secteur d'activité est obligatoire.");
        }

        String trimmedName = request.getName().trim();
        if (!org.getName().equals(trimmedName) && organizationRepository.existsByName(trimmedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une organisation avec ce nom existe déjà.");
        }

        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secteur invalide avec l'identifiant: " + request.getSectorId()));

        org.setName(trimmedName);
        org.setSector(sector);

        return toOrganizationDTO(organizationRepository.save(org));
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée avec l'identifiant: " + id));

        // Check 1: Existing Evaluations
        List<Evaluation> evals = evaluationRepository.findByOrganization(org);
        if (evals != null && !evals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer cette organisation : elle possède " + evals.size() + " évaluation(s) enregistrée(s).");
        }

        // Check 2: Existing Users linked
        List<User> users = userRepository.findByOrganization(org);
        if (users != null && !users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer cette organisation : elle est rattachée à " + users.size() + " utilisateur(s).");
        }

        organizationRepository.delete(org);
    }

    // --- FIRM USER OPERATIONS ---

    @Transactional(readOnly = true)
    public List<FirmUserDTO> getAllFirmUsers() {
        return userRepository.findAll().stream()
                .filter(u -> "ROLE_USER".equals(u.getRole()))
                .map(this::toFirmUserDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FirmUserDTO getFirmUserById(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> "ROLE_USER".equals(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur organisation non trouvé avec l'identifiant: " + id));
        return toFirmUserDTO(user);
    }

    @Transactional
    public FirmUserDTO createFirmUser(FirmUserRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom d'utilisateur est obligatoire.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est obligatoire.");
        }
        if (request.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'organisation de rattachement est obligatoire.");
        }

        String trimmedUsername = request.getUsername().trim();
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom d'utilisateur est déjà utilisé.");
        }

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organisation invalide avec l'identifiant: " + request.getOrganizationId()));

        User user = User.builder()
                .username(trimmedUsername)
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .role("ROLE_USER")
                .organization(org)
                .organizationName(org.getName())
                .build();

        return toFirmUserDTO(userRepository.save(user));
    }

    @Transactional
    public FirmUserDTO updateFirmUser(Long id, FirmUserRequest request) {
        User user = userRepository.findById(id)
                .filter(u -> "ROLE_USER".equals(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur organisation non trouvé avec l'identifiant: " + id));

        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom d'utilisateur est obligatoire.");
        }
        if (request.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'organisation de rattachement est obligatoire.");
        }

        String trimmedUsername = request.getUsername().trim();
        if (!user.getUsername().equals(trimmedUsername) && userRepository.existsByUsername(trimmedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce nom d'utilisateur est déjà utilisé.");
        }

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organisation invalide avec l'identifiant: " + request.getOrganizationId()));

        user.setUsername(trimmedUsername);
        user.setOrganization(org);
        user.setOrganizationName(org.getName());

        // Update password if a new one was provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        return toFirmUserDTO(userRepository.save(user));
    }

    @Transactional
    public void deleteFirmUser(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> "ROLE_USER".equals(u.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur organisation non trouvé avec l'identifiant: " + id));

        List<EvaluationProject> assignedProjects = evaluationProjectRepository.findByAssignedUser(user);
        if (assignedProjects != null && !assignedProjects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer cet utilisateur : il est assigné à " + assignedProjects.size() + " projet(s) d'évaluation.");
        }

        userRepository.delete(user);
    }

    // --- MAPPERS ---

    private OrganizationDTO toOrganizationDTO(Organization org) {
        int userCount = userRepository.findByOrganization(org).size();
        return OrganizationDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .sectorId(org.getSector() != null ? org.getSector().getId() : null)
                .sectorName(org.getSector() != null ? org.getSector().getName() : null)
                .userCount(userCount)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    // --- EVALUATION OPERATIONS ---

    @Transactional
    public List<EvaluationDTO> getAllEvaluations() {
        List<Evaluation> evals = evaluationRepository.findAll();
        for (Evaluation eval : evals) {
            ensureEvaluationItems(eval);
        }
        return evals.stream()
                .map(this::toEvaluationDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EvaluationDTO getEvaluationById(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée avec l'identifiant: " + id));
        ensureEvaluationItems(eval);
        return toEvaluationDTO(eval);
    }

    @Transactional
    public EvaluationDTO createEvaluation(EvaluationRequest request) {
        if (request.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'organisation est obligatoire.");
        }

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organisation invalide avec l'identifiant: " + request.getOrganizationId()));

        Evaluation eval = Evaluation.builder()
                .organization(org)
                .status(EvaluationStatus.DRAFT)
                .build();

        eval = evaluationRepository.save(eval);

        // Populate EvaluationItems from all 93 master SecurityActions with resolved sector coefficients
        List<SecurityAction> actions = securityActionRepository.findAll();
        // Sort actions consistently by numeric code (A01 -> A93)
        actions.sort(java.util.Comparator.comparing(a -> {
            try {
                return Integer.parseInt(a.getCode().replaceAll("\\D+", ""));
            } catch (Exception e) {
                return 999;
            }
        }));

        Sector sector = org.getSector();

        List<EvaluationItem> items = new java.util.ArrayList<>();
        for (SecurityAction action : actions) {
            Integer defaultCoeff = 3;
            if (sector != null) {
                defaultCoeff = coefficientRepository.findByActionAndSector(action, sector)
                        .map(ActionSectorCoefficient::getCoefficient)
                        .orElse(3);
            }

            EvaluationItem item = EvaluationItem.builder()
                    .evaluation(eval)
                    .securityAction(action)
                    .implementationStatus(ImplementationStatus.NOT_PLANNED)
                    .coefficient(defaultCoeff)
                    .build();
            items.add(item);
        }

        evaluationItemRepository.saveAll(items);
        eval.setItems(items);

        return toEvaluationDTO(eval);
    }

    @Transactional
    public EvaluationDTO updateItemCoefficient(Long evalId, Long itemId, Integer newCoefficient) {
        Evaluation eval = evaluationRepository.findById(evalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée"));

        if (newCoefficient == null || newCoefficient < 1 || newCoefficient > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le coefficient doit être compris entre 1 et 5.");
        }

        EvaluationItem item = evaluationItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément d'évaluation non trouvé"));

        if (!item.getEvaluation().getId().equals(evalId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet élément n'appartient pas à l'évaluation spécifiée.");
        }

        item.setCoefficient(newCoefficient);
        evaluationItemRepository.save(item);

        return toEvaluationDTO(evaluationRepository.findById(evalId).get());
    }

    @Transactional
    public EvaluationDTO resetItemCoefficientToSectorDefault(Long evalId, Long itemId) {
        Evaluation eval = evaluationRepository.findById(evalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée"));

        EvaluationItem item = evaluationItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément d'évaluation non trouvé"));

        if (!item.getEvaluation().getId().equals(evalId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet élément n'appartient pas à l'évaluation spécifiée.");
        }

        Sector sector = eval.getOrganization() != null ? eval.getOrganization().getSector() : null;
        Integer defaultCoeff = 3;
        if (sector != null && item.getSecurityAction() != null) {
            defaultCoeff = coefficientRepository.findByActionAndSector(item.getSecurityAction(), sector)
                    .map(ActionSectorCoefficient::getCoefficient)
                    .orElse(3);
        }

        item.setCoefficient(defaultCoeff);
        evaluationItemRepository.save(item);

        return toEvaluationDTO(evaluationRepository.findById(evalId).get());
    }

    @Transactional
    public EvaluationDTO updateEvaluationStatus(Long id, EvaluationRequest request) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée avec l'identifiant: " + id));

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut est obligatoire.");
        }

        try {
            EvaluationStatus newStatus = EvaluationStatus.valueOf(request.getStatus().toUpperCase());
            eval.setStatus(newStatus);
            if (newStatus == EvaluationStatus.ASSIGNED && eval.getStartedAt() == null) {
                eval.setStartedAt(java.time.LocalDateTime.now());
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide: " + request.getStatus());
        }

        // Ensure the complete 93-rule assessment package is present on transmission
        ensureEvaluationItems(eval);

        return toEvaluationDTO(evaluationRepository.save(eval));
    }

    @Transactional
    public void ensureEvaluationItems(Evaluation eval) {
        if (eval == null) return;
        List<EvaluationItem> existingItems = eval.getItems();
        if (existingItems == null) {
            existingItems = new java.util.ArrayList<>();
            eval.setItems(existingItems);
        }

        List<SecurityAction> allActions = securityActionRepository.findAll();
        if (allActions.isEmpty()) {
            return;
        }

        if (existingItems.size() >= allActions.size()) {
            return;
        }

        java.util.Set<Long> existingActionIds = existingItems.stream()
                .filter(i -> i.getSecurityAction() != null)
                .map(i -> i.getSecurityAction().getId())
                .collect(Collectors.toSet());

        Sector sector = eval.getOrganization() != null ? eval.getOrganization().getSector() : null;
        List<EvaluationItem> newItems = new java.util.ArrayList<>();

        // Sort all actions numerically by code (A01 -> A93)
        allActions.sort(java.util.Comparator.comparing(a -> {
            try {
                return Integer.parseInt(a.getCode().replaceAll("\\D+", ""));
            } catch (Exception e) {
                return 999;
            }
        }));

        for (SecurityAction action : allActions) {
            if (!existingActionIds.contains(action.getId())) {
                Integer defaultCoeff = 3;
                if (sector != null) {
                    defaultCoeff = coefficientRepository.findByActionAndSector(action, sector)
                            .map(ActionSectorCoefficient::getCoefficient)
                            .orElse(3);
                }

                EvaluationItem item = EvaluationItem.builder()
                        .evaluation(eval)
                        .securityAction(action)
                        .implementationStatus(ImplementationStatus.NOT_PLANNED)
                        .coefficient(defaultCoeff)
                        .build();
                newItems.add(item);
            }
        }

        if (!newItems.isEmpty()) {
            evaluationItemRepository.saveAll(newItems);
            existingItems.addAll(newItems);
            eval.setItems(existingItems);
            evaluationRepository.save(eval);
        }
    }

    @Transactional(readOnly = true)
    public Organization findOrganizationByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return organizationRepository.findByName(name.trim()).orElse(null);
    }

    @Transactional
    public void deleteEvaluation(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée avec l'identifiant: " + id));

        // Delete associated items first if present
        if (eval.getItems() != null && !eval.getItems().isEmpty()) {
            evaluationItemRepository.deleteAll(eval.getItems());
        }

        evaluationRepository.delete(eval);
    }

    private EvaluationDTO toEvaluationDTO(Evaluation eval) {
        List<EvaluationItemDTO> itemDtos = null;
        if (eval.getItems() != null && !eval.getItems().isEmpty()) {
            Sector sector = eval.getOrganization() != null ? eval.getOrganization().getSector() : null;
            java.util.Map<Long, Integer> sectorDefaultCoeffs = new java.util.HashMap<>();
            if (sector != null) {
                List<ActionSectorCoefficient> coeffs = coefficientRepository.findBySector(sector);
                for (ActionSectorCoefficient c : coeffs) {
                    if (c.getAction() != null) {
                        sectorDefaultCoeffs.put(c.getAction().getId(), c.getCoefficient());
                    }
                }
            }

            itemDtos = eval.getItems().stream()
                    .sorted(java.util.Comparator.comparing(item -> {
                        if (item.getSecurityAction() == null || item.getSecurityAction().getCode() == null) return 999;
                        try {
                            return Integer.parseInt(item.getSecurityAction().getCode().replaceAll("\\D+", ""));
                        } catch (Exception e) {
                            return 999;
                        }
                    }))
                    .map(item -> {
                        Integer defaultCoeff = 3;
                        if (item.getSecurityAction() != null) {
                            defaultCoeff = sectorDefaultCoeffs.getOrDefault(item.getSecurityAction().getId(), 3);
                        }
                        return EvaluationItemDTO.builder()
                                .id(item.getId())
                                .securityActionId(item.getSecurityAction() != null ? item.getSecurityAction().getId() : null)
                                .actionCode(item.getSecurityAction() != null ? item.getSecurityAction().getCode() : null)
                                .isoControl(item.getSecurityAction() != null ? item.getSecurityAction().getIsoControl() : null)
                                .theme(item.getSecurityAction() != null ? item.getSecurityAction().getTheme() : null)
                                .projectSubdomain(item.getSecurityAction() != null ? item.getSecurityAction().getProjectSubdomain() : null)
                                .description(item.getSecurityAction() != null ? item.getSecurityAction().getDescription() : null)
                                .coefficient(item.getCoefficient())
                                .sectorDefaultCoefficient(defaultCoeff)
                                .implementationStatus(item.getImplementationStatus() != null ? item.getImplementationStatus().name() : null)
                                .justification(item.getJustification())
                                .comment(item.getComment())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return EvaluationDTO.builder()
                .id(eval.getId())
                .organizationId(eval.getOrganization() != null ? eval.getOrganization().getId() : null)
                .organizationName(eval.getOrganization() != null ? eval.getOrganization().getName() : null)
                .sectorId(eval.getOrganization() != null && eval.getOrganization().getSector() != null ? eval.getOrganization().getSector().getId() : null)
                .sectorName(eval.getOrganization() != null && eval.getOrganization().getSector() != null ? eval.getOrganization().getSector().getName() : null)
                .status(eval.getStatus() != null ? eval.getStatus().name() : null)
                .itemCount(eval.getItems() != null ? eval.getItems().size() : 0)
                .items(itemDtos)
                .startedAt(eval.getStartedAt())
                .submittedAt(eval.getSubmittedAt())
                .createdAt(eval.getCreatedAt())
                .updatedAt(eval.getUpdatedAt())
                .build();
    }

    @Transactional
    public EvaluationDTO validateEvaluation(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée avec l'identifiant: " + id));
        eval.setStatus(EvaluationStatus.COMPLETED);
        return toEvaluationDTO(evaluationRepository.save(eval));
    }

    @Transactional
    public EvaluationDTO returnEvaluationForRevision(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Évaluation non trouvée avec l'identifiant: " + id));
        eval.setStatus(EvaluationStatus.IN_PROGRESS);
        return toEvaluationDTO(evaluationRepository.save(eval));
    }

    // --- PSG REFERENCE ACTIONS & SECTOR COEFFICIENTS ---

    @Transactional(readOnly = true)
    public List<PsgActionDTO> getPsgActionsBySector(Long sectorId) {
        Sector sector = null;
        if (sectorId != null) {
            sector = sectorRepository.findById(sectorId).orElse(null);
        }

        java.util.Map<Long, Integer> coeffMap = new java.util.HashMap<>();
        if (sector != null) {
            for (ActionSectorCoefficient asc : coefficientRepository.findBySector(sector)) {
                if (asc.getAction() != null) {
                    coeffMap.put(asc.getAction().getId(), asc.getCoefficient());
                }
            }
        }

        final Sector finalSector = sector;
        return securityActionRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(SecurityAction::getCode))
                .map(action -> PsgActionDTO.builder()
                        .id(action.getId())
                        .code(action.getCode())
                        .isoControl(action.getIsoControl())
                        .theme(action.getTheme())
                        .projectSubdomain(action.getProjectSubdomain())
                        .description(action.getDescription())
                        .sectorId(finalSector != null ? finalSector.getId() : null)
                        .sectorName(finalSector != null ? finalSector.getName() : null)
                        .coefficient(coeffMap.getOrDefault(action.getId(), 3))
                        .build())
                .collect(Collectors.toList());
    }

    private FirmUserDTO toFirmUserDTO(User user) {
        return FirmUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : user.getOrganizationName())
                .sectorName(user.getOrganization() != null && user.getOrganization().getSector() != null
                        ? user.getOrganization().getSector().getName() : null)
                .build();
    }
}
