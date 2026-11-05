package com.campus.incident.repository;

import com.campus.incident.entity.Incident;
import com.campus.incident.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByReportedById(Long reportedById);

    List<Incident> findByAssignedToId(Long assignedToId);

    List<Incident> findByStatus(Incident.Status status);
}
