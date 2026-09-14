package com.cosre.cosre_backend.modules.incident.repository;
import com.cosre.cosre_backend.modules.incident.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<IncidentReport> findAllByOrderByCreatedAtDesc();
}
