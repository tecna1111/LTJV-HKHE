package com.cosre.cosre_backend.modules.incident.service;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationAccessService;
import com.cosre.cosre_backend.modules.incident.entity.IncidentReport;
import com.cosre.cosre_backend.modules.incident.repository.IncidentReportRepository;
import com.cosre.cosre_backend.modules.incident.event.IncidentReportedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class IncidentReportService {
    private final IncidentReportRepository reports;
    private final EvaluationAccessService access;
    private final ApplicationEventPublisher events;
    public record CreateRequest(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=5000) String description) {}
    public record UpdateRequest(@NotNull IncidentReport.Status status, @Size(max=2000) String resolution) {}
    @Transactional
    public IncidentReport create(CreateRequest request) {
        var report = new IncidentReport();
        report.setReporterId(access.currentUser().getId()); report.setTitle(request.title().trim());
        report.setDescription(request.description().trim()); report.setStatus(IncidentReport.Status.OPEN);
        report.setCreatedAt(LocalDateTime.now()); report.setUpdatedAt(report.getCreatedAt());
        report = reports.save(report);
        events.publishEvent(new IncidentReportedEvent(report.getId(), report.getReporterId(), report.getTitle(), report.getCreatedAt()));
        return report;
    }
    public List<IncidentReport> list() {
        var user = access.currentUser();
        return user.getRole() == RoleEnum.ADMIN ? reports.findAllByOrderByCreatedAtDesc() : reports.findByReporterIdOrderByCreatedAtDesc(user.getId());
    }
    public IncidentReport get(Long id) {
        var report = reports.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        var user = access.currentUser();
        if (user.getRole() != RoleEnum.ADMIN && !user.getId().equals(report.getReporterId())) throw new AccessDeniedException("No access to incident");
        return report;
    }
    @Transactional
    public IncidentReport update(Long id, UpdateRequest request) {
        var user = access.currentUser();
        if (user.getRole() != RoleEnum.ADMIN) throw new AccessDeniedException("Only Admin can handle incidents");
        var report = get(id);
        var next = request.status();
        boolean valid = report.getStatus() == next || switch (report.getStatus()) {
            case OPEN -> next == IncidentReport.Status.IN_PROGRESS;
            case IN_PROGRESS -> next == IncidentReport.Status.RESOLVED;
            case RESOLVED -> next == IncidentReport.Status.CLOSED || next == IncidentReport.Status.IN_PROGRESS;
            case CLOSED -> false;
        };
        if (!valid) throw new BusinessRuleException("Invalid incident status transition");
        if ((next == IncidentReport.Status.RESOLVED || next == IncidentReport.Status.CLOSED)
                && (request.resolution() == null || request.resolution().isBlank())) throw new BusinessRuleException("Resolution is required");
        report.setStatus(next); report.setResolution(request.resolution()); report.setHandledBy(user.getId());
        report.setUpdatedAt(LocalDateTime.now());
        return reports.save(report);
    }
}
