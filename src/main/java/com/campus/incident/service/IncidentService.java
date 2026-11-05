package com.campus.incident.service;

import com.campus.incident.entity.Incident;
import com.campus.incident.entity.User;
import com.campus.incident.repository.IncidentRepository;
import com.campus.incident.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    public Incident createIncident(String title, String description, Incident.Severity severity, Long reportedById) {
        User reportedBy = userRepository.findById(reportedById)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + reportedById));

        Incident incident = Incident.builder()
            .title(title)
            .description(description)
            .severity(severity)
            .status(Incident.Status.OPEN)
            .reportedBy(reportedBy)
            .assignedTo(null)
            .resolutionNotes(null)
            .build();

        return incidentRepository.save(incident);
    }

    public List<Incident> getIncidentsForUser(User user) {
        switch (user.getRole()) {
            case ADMIN:
                return incidentRepository.findAll();
            case OFFICER:
                return incidentRepository.findByAssignedToId(user.getId());
            case STUDENT:
            case STAFF:
            default:
                return incidentRepository.findByReportedById(user.getId());
        }
    }

    public Optional<Incident> getIncidentById(Long id) {
        return incidentRepository.findById(id);
    }

    public List<User> getOfficers() {
        return userRepository.findAllByRole(User.Role.OFFICER);
    }

    public Incident assignIncident(Long incidentId, Long officerId) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        User officer = userRepository.findById(officerId)
            .orElseThrow(() -> new IllegalArgumentException("Officer not found with id: " + officerId));

        if (officer.getRole() != User.Role.OFFICER) {
            throw new IllegalArgumentException("User is not a security officer");
        }

        incident.setAssignedTo(officer);
        if (incident.getStatus() == Incident.Status.OPEN) {
            incident.setStatus(Incident.Status.IN_PROGRESS);
        }
        return incidentRepository.save(incident);
    }

    public Incident updateIncidentStatus(Long incidentId, Incident.Status newStatus) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        incident.setStatus(newStatus);
        return incidentRepository.save(incident);
    }

    public Incident updateIncidentSeverity(Long incidentId, Incident.Severity newSeverity) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        incident.setSeverity(newSeverity);
        return incidentRepository.save(incident);
    }

    public Incident updateResolutionNotes(Long incidentId, String notes) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        incident.setResolutionNotes(notes);
        return incidentRepository.save(incident);
    }
}
