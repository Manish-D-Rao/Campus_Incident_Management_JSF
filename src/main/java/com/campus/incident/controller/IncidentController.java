package com.campus.incident.controller;

import com.campus.incident.entity.Incident;
import com.campus.incident.entity.User;
import com.campus.incident.repository.UserRepository;
import com.campus.incident.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;

    public IncidentController(IncidentService incidentService, UserRepository userRepository) {
        this.incidentService = incidentService;
        this.userRepository = userRepository;
    }

    public static class CreateIncidentRequest {
        public String title;
        public String description;
    }

    public static class AssignIncidentRequest {
        public Long officerId;
    }

    public static class OfficerOption {
        public Long id;
        public String name;
        public String email;

        public OfficerOption(User officer) {
            this.id = officer.getId();
            this.name = officer.getName();
            this.email = officer.getEmail();
        }
    }

    public static class UpdateStatusRequest {
        public String status;
    }

    public static class UpdateSeverityRequest {
        public String severity;
    }

    public static class UpdateNotesRequest {
        public String notes;
    }

    /**
     * Get the authenticated user from SecurityContext.
     * More reliable than @AuthenticationPrincipal with custom UserDetails implementation.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        // JwtAuthFilter already stores the database User as the principal.
        // Reuse it directly so admin role checks do not depend on a second lookup.
        if (principal instanceof User) {
            return (User) principal;
        }
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
            .orElse(null);
    }

    /**
     * Get incidents filtered by the logged-in user's role.
     * ADMIN sees all, OFFICER sees assigned, STUDENT/STAFF sees own reported incidents.
     */
    @GetMapping
    public ResponseEntity<List<Incident>> getIncidents() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (user.getRole() != User.Role.ADMIN
            && user.getRole() != User.Role.OFFICER
            && user.getRole() != User.Role.STUDENT
            && user.getRole() != User.Role.STAFF) {
            return ResponseEntity.status(403).build();
        }

        List<Incident> incidents = incidentService.getIncidentsForUser(user);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/officers")
    public ResponseEntity<List<OfficerOption>> getOfficers() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        List<OfficerOption> officers = incidentService.getOfficers().stream()
            .map(OfficerOption::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(officers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (user.getRole() != User.Role.ADMIN
            && user.getRole() != User.Role.OFFICER
            && user.getRole() != User.Role.STUDENT
            && user.getRole() != User.Role.STAFF) {
            return ResponseEntity.status(403).build();
        }

        return incidentService.getIncidentById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new incident. Any authenticated user can report.
     * Severity defaults to LOW — only ADMIN can prioritize later.
     */
    @PostMapping
    public ResponseEntity<Incident> createIncident(@RequestBody CreateIncidentRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Incident incident = incidentService.createIncident(
            request.title,
            request.description,
            Incident.Severity.LOW,
            user.getId()
        );
        return ResponseEntity.ok(incident);
    }

    /**
     * Assign an incident to a security officer. ADMIN only.
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignIncident(@PathVariable Long id,
                                            @RequestBody AssignIncidentRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Admin role required; server resolved your role as " + user.getRole() + "."));
        }

        try {
            Incident incident = incidentService.assignIncident(id, request.officerId);
            return ResponseEntity.ok(incident);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    /**
     * Update incident severity/priority. ADMIN only.
     */
    @PutMapping("/{id}/severity")
    public ResponseEntity<Incident> updateSeverity(@PathVariable Long id,
                                                   @RequestBody UpdateSeverityRequest request) {
        User user = getCurrentUser();
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).body(null);
        }

        Incident.Severity severity = Incident.Severity.valueOf(request.severity.toUpperCase());
        Incident incident = incidentService.updateIncidentSeverity(id, severity);
        return ResponseEntity.ok(incident);
    }

    /**
     * Update incident status. ADMIN or OFFICER can change status.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Incident> updateStatus(@PathVariable Long id,
                                                 @RequestBody UpdateStatusRequest request) {
        User user = getCurrentUser();
        if (user == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.OFFICER)) {
            return ResponseEntity.status(403).body(null);
        }

        Incident.Status status = Incident.Status.valueOf(request.status.toUpperCase());
        Incident incident = incidentService.updateIncidentStatus(id, status);
        return ResponseEntity.ok(incident);
    }

    /**
     * Update resolution notes. OFFICER only.
     */
    @PutMapping("/{id}/notes")
    public ResponseEntity<Incident> updateNotes(@PathVariable Long id,
                                                @RequestBody UpdateNotesRequest request) {
        User user = getCurrentUser();
        if (user == null || user.getRole() != User.Role.OFFICER) {
            return ResponseEntity.status(403).body(null);
        }

        Incident incident = incidentService.updateResolutionNotes(id, request.notes);
        return ResponseEntity.ok(incident);
    }
}
