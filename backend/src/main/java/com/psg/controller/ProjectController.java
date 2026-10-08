package com.psg.controller;

import com.psg.model.EvaluationProject;
import com.psg.model.User;
import com.psg.repository.EvaluationProjectRepository;
import com.psg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ProjectController {

    @Autowired
    private EvaluationProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // --- ADMIN ENDPOINTS ---

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> "ROLE_USER".equals(u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/admin/projects")
    public ResponseEntity<?> createProject(@RequestBody EvaluationProject projectRequest) {
        if (projectRequest.getAssignedUser() == null || projectRequest.getAssignedUser().getId() == null) {
            return ResponseEntity.badRequest().body("Assigned user is required.");
        }
        
        User user = userRepository.findById(projectRequest.getAssignedUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        EvaluationProject project = EvaluationProject.builder()
                .title(projectRequest.getTitle())
                .description(projectRequest.getDescription())
                .assignedUser(user)
                .deadline(projectRequest.getDeadline())
                .status("CREATED")
                .build();
                
        EvaluationProject saved = projectRepository.save(project);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admin/projects")
    public ResponseEntity<List<EvaluationProject>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    // --- USER (FIRM) ENDPOINTS ---

    @GetMapping("/user/projects")
    public ResponseEntity<List<EvaluationProject>> getMyProjects(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found"));
        return ResponseEntity.ok(projectRepository.findByAssignedUser(user));
    }

    @PutMapping("/user/projects/{id}")
    public ResponseEntity<?> submitEvaluation(
            @PathVariable Long id, 
            @RequestBody EvaluationProject submission, 
            Principal principal) {
        
        EvaluationProject project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
                
        if (!project.getAssignedUser().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Not authorized to fill this project.");
        }

        project.setPhysicalSecurityStatus(submission.getPhysicalSecurityStatus());
        project.setNetworkSecurityStatus(submission.getNetworkSecurityStatus());
        project.setDataProtectionStatus(submission.getDataProtectionStatus());
        project.setComments(submission.getComments());
        project.setStatus("SUBMITTED");

        EvaluationProject saved = projectRepository.save(project);
        return ResponseEntity.ok(saved);
    }
}
